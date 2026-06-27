/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.model.builtins;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState.BuiltInBlockStateHandler;

public class BuiltInBlockStateRegistry {
	
	public static interface IBlockStateConstructor{
		
		public BlockState construct(String name, int dataVersion);
		
	}
	
	public static class DefaultBlockStateConstructor implements IBlockStateConstructor{
		
		private Class<? extends BlockState> clazz;
		
		public DefaultBlockStateConstructor(Class<? extends BlockState> clazz) {
			this.clazz = clazz;
		}
		
		@Override
		public BlockState construct(String name, int dataVersion) {
			try {
				return clazz.getConstructor(String.class, int.class).newInstance(name, dataVersion);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			return null;
		}
		
	}
	
	// Статическое поле для хранения регистрации
	public static Map<String, IBlockStateConstructor> builtins = new HashMap<String, IBlockStateConstructor>();
	
	// Единственный метод load()
	public static void load(){
		// Сначала загружаем встроенные обработчики (если они есть)
		BuiltInBlockState.load();
		
		// Создаём временную карту для регистрации
		Map<String, IBlockStateConstructor> builtins = new HashMap<String, IBlockStateConstructor>();
		
		// --- СТАНДАРТНЫЕ РЕГИСТРАЦИИ ---
		builtins.put("dragon_head", new DefaultBlockStateConstructor(BlockStateSkull.class));
		builtins.put("dragon_wall_head", new DefaultBlockStateConstructor(BlockStateSkull.class));
		
		builtins.put("minecraft:banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:white_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:orange_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:magenta_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:light_blue_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:yellow_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:lime_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:pink_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:gray_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:light_gray_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:cyan_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:purple_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:blue_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:brown_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:green_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:red_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:black_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:white_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:orange_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:magenta_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:light_blue_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:yellow_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:lime_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:pink_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:gray_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:light_gray_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:cyan_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:purple_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:blue_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:brown_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:green_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:red_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		builtins.put("minecraft:black_wall_banner", new DefaultBlockStateConstructor(BlockStateBanner.class));
		
		// Регистрация жидких блоков
		for(String liquidType : Config.liquid)
			builtins.put(liquidType, new DefaultBlockStateConstructor(BlockStateLiquid.class));
		
		// --- НОВАЯ СТРОКА ДЛЯ CHISELS & BITS ---
		// ВАЖНО: Убедись, что имя "chiselsandbits:chiseled" совпадает с тем, что в NBT.
		// И что класс BuiltInBlockStateChiseledBlock.java существует и правильно назван.
		builtins.put("chiselsandbits:chiseled", new DefaultBlockStateConstructor(BuiltInBlockStateChiseledBlock.class));
		// --- КОНЕЦ НОВОЙ СТРОКИ ---
		

		// Присваиваем временную карту в статическое поле
		BuiltInBlockStateRegistry.builtins = builtins;
	}
	
	// Метод для создания нового BlockState
	public static BlockState newBlockState(String name, int dataVersion) {
		// Сначала проверяем, есть ли у нас встроенный обработчик (из BuiltInBlockState.getHandler)
		BuiltInBlockStateHandler builtInBlockStateHandler = BuiltInBlockState.getHandler(name);
		if(builtInBlockStateHandler != null) {
			// У нас есть пользовательская реализация.
			return new BuiltInBlockState(name, dataVersion, builtInBlockStateHandler);
		}
		
		// Иначе ищем в карте builtins
		IBlockStateConstructor constructor = builtins.get(name);
		if (constructor != null) {
			return constructor.construct(name, dataVersion);
		}
		
		// Если ничего не нашли, возвращаем null или базовый BlockState
		// (в оригинальном коде тут, скорее всего, создавался бы стандартный BlockState)
		return null; // или реализуй создание стандартного BlockState, если constructor == null
	}

}
