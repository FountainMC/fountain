package org.fountainmc.world.block;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;

import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.techcable.pineapple.SneakyThrow;
import org.fountainmc.WetServer;
import org.fountainmc.api.BlockType;
import org.fountainmc.api.world.block.BlockState;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

public class WetBlockState implements BlockState {

    private final WetServer server;
    @Getter private final IBlockState handle;

    @Getter(lazy = true) private static final ImmutableMap<Block, BiFunction<WetServer, IBlockState, ? extends WetBlockState>> factories =
            scanClasspath();

    @SneakyThrows(IllegalAccessException.class)
    private static ImmutableMap<Block, BiFunction<WetServer, IBlockState, ? extends WetBlockState>> scanClasspath() {
        ImmutableMap.Builder<Block, BiFunction<WetServer, IBlockState, ? extends WetBlockState>> builder = ImmutableMap.builder();
        Reflections reflections = new Reflections(getReflectionsConfiguration("org.fountainmc.world.block"));
        Set<Class<?>> types = reflections.getTypesAnnotatedWith(BlockStateImpl.class);
        if (types != null) {
            for (Class<?> type : types) {
                Verify.verify(WetBlockState.class.isAssignableFrom(type), "Class %s isn't instanceof WetBlockState", type.getTypeName());
                for (String blockName : ImmutableList.copyOf(type.getAnnotation(BlockStateImpl.class).value())) {
                    System.out.println(Block.getBlockFromName("minecraft:chest"));
                    System.out.println(Block.getBlockFromName("minecraft:"+blockName));
                    Block block = Verify.verifyNotNull(Block.getBlockFromName(blockName),
                            "Class %s specified unknown block name minecraft:%s.", type.getTypeName(), blockName);
                    final MethodHandle constructorHandle;
                    try {
                        constructorHandle =
                                MethodHandles.publicLookup().findConstructor(type, MethodType.methodType(type, WetServer.class, IBlockState.class));
                    } catch (NoSuchMethodException e) {
                        throw new VerifyException("Can't find constructor for " + type.getTypeName());
                    }
                    builder.put(block, (server, state) -> {
                        try {
                            return (WetBlockState) constructorHandle.invoke(server, state);
                        } catch (Throwable throwable) {
                            throw SneakyThrow.sneakyThrow(throwable);
                        }
                    });
                }
            }
        }
        return builder.build();
    }

    public static ConfigurationBuilder getReflectionsConfiguration(String packageName) {
        return new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(packageName))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName + ".")))
                .setScanners(new TypeElementsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
    }

    public static WetBlockState createState(WetServer server, IBlockState handle) {
        return getFactories().getOrDefault(checkNotNull(handle, "Null block state").getBlock(), WetBlockState::new).apply(server, handle);
    }

    protected WetBlockState(WetServer server, IBlockState handle) {
        this.server = checkNotNull(server, "Null server");
        this.handle = checkNotNull(handle, "Null state");
    }

    @Nonnull
    @Override
    public BlockType getBlockType() {
        return handle.getBlock().getFountainType();
    }

}
