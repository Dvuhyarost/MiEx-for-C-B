package nl.bramstout.mcworldexporter.model.builtins;

import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import net.jpountz.lz4.LZ4FrameInputStream;

import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace; // <-- Добавлен импорт
import nl.bramstout.mcworldexporter.model.Direction; // <-- Добавлен импорт
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagByteArray;
import nl.bramstout.mcworldexporter.model.BlockState;

import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;
import nl.bramstout.mcworldexporter.lighting.BlockLightValues;

public class BuiltInBlockStateChiseledBlock extends BuiltInBlockState {

    private static final int MAX_BLOCKS_TO_DUMP = 1;
    private static int blocksDumped = 0;

    private static final ThreadLocal<Set<String>> processingNames = ThreadLocal.withInitial(HashSet::new);

    public BuiltInBlockStateChiseledBlock(String name, int dataVersion) {
        super("chiselsandbits:chiseled_block", dataVersion, new ChiseledBlockHandler());
    }

    private static class ChiseledBlockHandler extends BuiltInBlockState.BuiltInBlockStateHandler {

        public ChiseledBlockHandler() {
            super("", 0, new com.google.gson.JsonObject());
        }

        @Override
        public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, int layer, BlockState state) {
            String currentName = state.getName();
            Set<String> currentStack = processingNames.get();
            boolean tracking = false;

            if (!currentStack.add(currentName)) {
                return createEmptyBakedState(state);
            }
            tracking = true;

            List<List<Model>> allModels = new ArrayList<>();

            try {
                if (blocksDumped == 0) {
                    System.out.println("[C&B] Processing block at " + x + "," + y + "," + z);
                }

                NbtTag dataTag = properties.get("data");
                if (!(dataTag instanceof NbtTagCompound)) return createEmptyBakedState(state);
                NbtTagCompound data = (NbtTagCompound) dataTag;

                NbtTag payloadTag = data.get("payload");
                if (!(payloadTag instanceof NbtTagByteArray)) return createEmptyBakedState(state);
                byte[] compressedBytes = ((NbtTagByteArray) payloadTag).getData();
                if (compressedBytes.length == 0) return createEmptyBakedState(state);

                byte[] decompressedBytes = decompressPayload(compressedBytes);
                if (decompressedBytes == null || decompressedBytes.length == 0) {
                    System.err.println("[C&B] Decompression failed.");
                    return createEmptyBakedState(state);
                }
                
                if (blocksDumped < MAX_BLOCKS_TO_DUMP) {
                    System.out.println("[C&B] Decompressed: " + compressedBytes.length + " -> " + decompressedBytes.length + " bytes");
                    blocksDumped++;
                }

                CBStorage storage = parseCBStorageSearch(decompressedBytes);
                
                if (storage != null && !storage.palette.isEmpty()) {
                    int paletteSize = storage.palette.size();
                    int bitsPerVoxel = (paletteSize > 1) ? 32 - Integer.numberOfLeadingZeros(paletteSize - 1) : 1;
                    int totalVoxels = 16 * 16 * 16; 
                    
                    float voxelScale = 1.0f / 16.0f;
                    float voxelSpacing = 16 / 16.0f;
                    int generatedCount = 0; 

                    for (int i = 0; i < totalVoxels; i++) {
                        int paletteIndex = getBitsFromSet(storage.bitSet, i * storage.bitsPerVoxel, storage.bitsPerVoxel);
                        if (paletteIndex == 0) continue; 
                        if (paletteIndex >= storage.palette.size()) continue; 

                        String blockName = storage.palette.get(paletteIndex);
                        if (blockName == null || blockName.isEmpty()) continue;

                        int innerId = BlockStateRegistry.getIdForName(blockName, state.getDataVersion());
                        if (innerId == -1) continue;

                        BlockState innerState = BlockStateRegistry.getState(innerId);
                        if (!innerState.getName().equals(blockName)) continue;

                        BakedBlockState innerBaked = innerState.getBakedBlockState(
                            new NbtTagCompound(), 0, 0, 0, layer, false
                        );
                        if (innerBaked == null) continue;

                        int cx = i & 15;          
                        int cy = (i >> 4) & 15;   
                        int cz = (i >> 8) & 15;   

                        List<Model> src = new ArrayList<>();
                        innerBaked.getModels(0, 0, 0, src); 

                        for (Model m : src) {
                            Model voxel = deepCopyModel(m);
                            if (voxel == null) continue;
 
                            // 1. Масштабирование геометрии
                            voxel.scale(voxelScale);
                            
                            // 2. Корректировка UV координат для 1/16 части текстуры
                            // Важно делать это ДО поворота (rotate), чтобы привязка шла к оригинальным осям блока
                            adjustUVsForVoxel(voxel, cx, cy, cz);
                            
                            // 3. Перемещение
                            float finalX = -(cx * voxelSpacing - 7.5f);
                            float finalY = cy * voxelSpacing - 7.5f;
                            float finalZ = cz * voxelSpacing - 7.5f;
                            voxel.translate(finalX, finalY, finalZ);
                            
                            // 4. Поворот
                            voxel.rotate(0.0f, 270.0f, 0.0f); 
                            
                            if (generatedCount < 5) {
                                System.out.println(String.format(
                                    "[DEBUG] Voxel #%d: %s | cx=%d cy=%d cz=%d | pos=(%.4f, %.4f, %.4f)",
                                    generatedCount, blockName, cx, cy, cz, finalX, finalY, finalZ
                                ));
                            }
                            
                            List<Model> singleModelList = new ArrayList<>();
                            singleModelList.add(voxel);
                            allModels.add(singleModelList);
                            
                            generatedCount++; 
                        }
                    }
                    System.out.println("[C&B] SUCCESS: Generated " + generatedCount + " voxel models.");
                }

            } catch (Throwable t) {
                System.err.println("[C&B] CRITICAL ERROR at " + x + "," + y + "," + z);
                t.printStackTrace();
            } finally {
                if (tracking) {
                    currentStack.remove(currentName);
                }
            }

            return new BakedBlockState(
                state.getName(), allModels,
                false, false, false, false, false, false,
                false, false, false, false, false, false,
                false, 0, false,
                (TintLayers) null, false, false,
                (BlockLightValues) null,
                (nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler) null
            );
        }

        /**
         * Корректирует UV-координаты модели так, чтобы они занимали только 1/16 часть 
         * исходной текстуры, соответствующую позиции вокселя (cx, cy, cz).
         */
        private void adjustUVsForVoxel(Model model, int cx, int cy, int cz) {
            // model.getFaces() возвращает сырой List, поэтому итерируемся через Object
            for (Object faceObj : model.getFaces()) {
                if (!(faceObj instanceof ModelFace)) continue;
                ModelFace face = (ModelFace) faceObj;
                
                float[] uvs = face.getUVs();
                if (uvs == null || uvs.length < 8) continue;
                
                Direction dir = face.getDirection();
                if (dir == null) continue;
                
                // Находим реальные границы UV для этой грани (с учетом возможных поворотов в самом JSON)
                float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
                float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
                
                for (int i = 0; i < 8; i += 2) {
                    minU = Math.min(minU, uvs[i]);
                    maxU = Math.max(maxU, uvs[i]);
                    minV = Math.min(minV, uvs[i+1]);
                    maxV = Math.max(maxV, uvs[i+1]);
                }
                
                float fU_min = 0, fU_max = 0;
                float fV_min = 0, fV_max = 0;
                
                // Определяем, какие оси мира соответствуют U и V в зависимости от направления грани
                switch (dir) {
                    case UP:
                    case DOWN:
                        fU_min = cx / 16.0f; fU_max = (cx + 1) / 16.0f;
                        fV_min = cz / 16.0f; fV_max = (cz + 1) / 16.0f;
                        break;
                    case NORTH:
                    case SOUTH:
                        fU_min = cx / 16.0f; fU_max = (cx + 1) / 16.0f;
                        fV_min = cy / 16.0f; fV_max = (cy + 1) / 16.0f;
                        break;
                    case WEST:
                    case EAST:
                        fU_min = cz / 16.0f; fU_max = (cz + 1) / 16.0f;
                        fV_min = cy / 16.0f; fV_max = (cy + 1) / 16.0f;
                        break;
                }
                
                // Вычисляем новый диапазон UV для 1/16 части текстуры
                float newMinU = minU + (maxU - minU) * fU_min;
                float newMaxU = minU + (maxU - minU) * fU_max;
                float newMinV = minV + (maxV - minV) * fV_min;
                float newMaxV = minV + (maxV - minV) * fV_max;
                
                // Масштабируем каждую UV координату в новый диапазон
                // Это корректно работает даже если UV были повернуты (rotation) в модели
                for (int i = 0; i < 8; i += 2) {
                    float u = uvs[i];
                    float v = uvs[i+1];
                    
                    float fU = (maxU > minU) ? (u - minU) / (maxU - minU) : 0;
                    float fV = (maxV > minV) ? (v - minV) / (maxV - minV) : 0;
                    
                    uvs[i]   = newMinU + fU * (newMaxU - newMinU);
                    uvs[i+1] = newMinV + fV * (newMaxV - newMinV);
                }
            }
        }

        private Model deepCopyModel(Model original) {
            if (original == null) return null;
            
            // Попытка 1: Метод copy()
            try {
                java.lang.reflect.Method copyMethod = original.getClass().getMethod("copy");
                Model copy = (Model) copyMethod.invoke(original);
                return copy;
            } catch (Exception e1) {
                // Игнорируем
            }
            
            // Попытка 2: Конструктор копирования Model(Model)
            try {
                java.lang.reflect.Constructor<?> ctor = 
                    original.getClass().getConstructor(original.getClass());
                Model copy = (Model) ctor.newInstance(original);
                return copy;
            } catch (Exception e2) {
                // Игнорируем
            }
            
            // Попытка 3: Стандартный конструктор new Model(m)
            try {
                Model copy = new Model(original);
                return copy;
            } catch (Exception e3) {
                System.err.println("[C&B] ERROR: All copy methods failed!");
                return null;
            }
        }

        private sun.misc.Unsafe getUnsafe() {
            try {
                java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (sun.misc.Unsafe) theUnsafe.get(null);
            } catch (Exception e) {
                throw new RuntimeException("Could not get Unsafe instance", e);
            }
        }

        private static class CBStorage {
            List<String> palette = new ArrayList<>();
            BitSet bitSet = new BitSet();
            int bitsPerVoxel = 1; 
        }

        private CBStorage parseCBStorageSearch(byte[] data) {
            try {
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
                CBStorage storage = new CBStorage();

                int firstByteInt = dis.readUnsignedByte(); 
                if (firstByteInt != 0x0A) {
                    dis = new DataInputStream(new ByteArrayInputStream(data)); 
                }

                byte rootType = dis.readByte();
                if (rootType != 10) return null;

                String rootName = readString(dis); 
                if (!"storage".equals(rootName)) return null;

                boolean foundData = false;
                boolean foundPalette = false;
                while (true) {
                    byte tagType = dis.readByte();
                    if (tagType == 0) break; 
                    String tagName = readString(dis);

                    if ("data".equals(tagName)) {
                        if (tagType == 12) { 
                            int len = dis.readInt(); 
                            long[] longs = new long[len];
                            for (int i = 0; i < len; i++) {
                                longs[i] = dis.readLong(); 
                            }
                            storage.bitSet = BitSet.valueOf(longs);
                            foundData = true;
                        } else if (tagType == 7) { 
                            int len = dis.readInt(); 
                            byte[] bytes = new byte[len];
                            dis.readFully(bytes);
                            storage.bitSet = BitSet.valueOf(bytes);
                            foundData = true;
                        } else {
                            skipTag(dis, tagType); 
                        }
                    } else if ("palette".equals(tagName) && tagType == 10) { 
                        while (true) {
                            byte innerType = dis.readByte();
                            if (innerType == 0) break; 
                            String innerName = readString(dis);

                            if ("entries".equals(innerName) && innerType == 9) { 
                                byte listType = dis.readByte(); 
                                int listLen = dis.readInt(); 

                                if (listType == 10) { 
                                    for (int i = 0; i < listLen; i++) {
                                        String blockName = extractBlockNameFromCompound(dis);
                                        if (blockName != null && !blockName.isEmpty()) {
                                            storage.palette.add(blockName);
                                        } else {
                                            storage.palette.add("minecraft:air"); 
                                        }
                                    }
                                    foundPalette = true;
                                } else {
                                    for (int i = 0; i < listLen; i++) skipTag(dis, listType);
                                }
                            } else {
                                skipTag(dis, innerType);
                            }
                        }
                    } else {
                        skipTag(dis, tagType);
                    }
                }

                if (!foundData || !foundPalette || storage.palette.isEmpty()) {
                    return null;
                }

                storage.bitsPerVoxel = storage.palette.size() > 1 ? 32 - Integer.numberOfLeadingZeros(storage.palette.size() - 1) : 1;
                return storage;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private String extractBlockNameFromCompound(DataInputStream dis) throws IOException {
            String foundName = null;
            while (true) {
                byte type = dis.readByte();
                if (type == 0) break; 
                String name = readString(dis);

                if ("state".equals(name) && type == 10) { 
                    String nestedName = extractBlockNameFromCompound(dis);
                    if (nestedName != null) foundName = nestedName;
                } else if ("Name".equals(name) && type == 8) { 
                    foundName = readString(dis);
                } else {
                    skipTag(dis, type);
                }
            }
            return foundName;
        }

        private String readString(DataInputStream dis) throws IOException {
            short len = dis.readShort(); 
            if (len <= 0) return "";
            byte[] buf = new byte[len];
            dis.readFully(buf);
            return new String(buf, StandardCharsets.UTF_8);
        }

        private void skipTag(DataInputStream dis, byte type) throws IOException {
            try {
                switch (type) {
                    case 1: dis.readByte(); break;
                    case 2: dis.readShort(); break;
                    case 3: dis.readInt(); break;
                    case 4: dis.readLong(); break;
                    case 5: dis.readFloat(); break;
                    case 6: dis.readDouble(); break;
                    case 7: { 
                        int len = dis.readInt(); 
                        if (len > 0) dis.skipBytes(len); 
                        break;
                    }
                    case 8: readString(dis); break; 
                    case 9: { 
                        byte elemType = dis.readByte(); 
                        int listLen = dis.readInt(); 
                        for (int i = 0; i < listLen; i++) skipTag(dis, elemType); 
                        break;
                    }
                    case 10: { 
                        while (true) {
                            byte t = dis.readByte(); 
                            if (t == 0) break; 
                            readString(dis); 
                            skipTag(dis, t); 
                        }
                        break;
                    }
                    case 11: { 
                        int len = dis.readInt(); 
                        if (len > 0) dis.skipBytes(len * 4); 
                        break;
                    }
                    case 12: { 
                        int len = dis.readInt(); 
                        if (len > 0) dis.skipBytes(len * 8); 
                        break;
                    }
                }
            } catch (EOFException e) {
            }
        }

        private int getBitsFromSet(BitSet bitSet, int offset, int length) {
            int result = 0;
            for (int i = 0; i < length; i++) {
                if (bitSet.get(offset + i)) result |= (1 << i);
            }
            return result;
        }

        private byte[] decompressPayload(byte[] compressed) throws IOException {
            if (compressed.length == 0) return null;
            boolean isLZ4Frame = (compressed[0] == 0x18 && compressed[1] == 0x4D && compressed[2] == 0x22 && compressed[3] == 0x04) ||
                                 (compressed[0] == 0x04 && compressed[1] == 0x22 && compressed[2] == 0x4D && compressed[3] == 0x18);

            if (isLZ4Frame) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
                     LZ4FrameInputStream lz4In = new LZ4FrameInputStream(bis);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = lz4In.read(buf)) > 0) baos.write(buf, 0, r);
                    return baos.toByteArray();
                }
            }
            return null;
        }

        private BakedBlockState createEmptyBakedState(BlockState state) {
            List<List<Model>> empty = new ArrayList<>();
            return new BakedBlockState(state.getName(), empty, false, false, false, false, false, false, false, false, false, false, false, false, false, 0, false, (TintLayers) null, false, false, (BlockLightValues) null, (nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler) null);
        }
    }
}