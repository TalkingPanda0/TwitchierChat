package dev.talkingpanda.twitchierchat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.talkingpanda.twitchierchat.TwitchierChat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.Filterable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    protected abstract Filterable<String> filterableFromOutgoing(FilteredText text);

    @ModifyArg(method = "signBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"),index = 1)
    private ItemStack formatBookTitle(ItemStack writtenBook, @Local(argsOnly = true,name="title") FilteredText title) {
        var formatted = TwitchierChat.formatString(title.raw());
        if(formatted == null) return writtenBook;

        writtenBook.set(DataComponents.CUSTOM_NAME, formatted);
        return writtenBook;
    }

    @ModifyVariable(method = "signBook", at = @At("STORE"), name = "pages")
    private List<Filterable<Component>> FormatBookContent(List<Filterable<Component>> pages, @Local(argsOnly = true, name = "contents") List<FilteredText> contents){
        return contents.stream().map(page -> this.filterableFromOutgoing(page).map(str -> {
            final Component text = TwitchierChat.formatString(str);
            return text == null ? Component.literal(str) : text;
        })).toList() ;
    }
}
