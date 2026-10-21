package com.playerpicked.playershop.utils;

import com.playerpicked.playershop.identifiers.keys.MessageKeys;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiConsumer;

@UtilityClass
@SuppressWarnings("UnstableApiUsage")
public final class DialogUtil {

    private static final String INPUT_KEY = "price";

    @NotNull
    public Dialog createPriceDialog(@NotNull BiConsumer<DialogResponseView, Object> callback) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(ComponentUtil.of(MessageKeys.DIALOG_PRICE_TITLE.getMessage()))
                        .body(MessageKeys.DIALOG_PRICE_BODY.getMessages().stream()
                                .map(line -> DialogBody.plainMessage(ComponentUtil.of(line)))
                                .toList())
                        .inputs(List.of(
                                DialogInput.text(
                                        INPUT_KEY,
                                        ComponentUtil.of(MessageKeys.DIALOG_PRICE_INPUT.getMessage())
                                ).build()
                        ))
                        .build())

                .type(DialogType.confirmation(
                        ActionButton.create(
                                ComponentUtil.of(MessageKeys.DIALOG_CONFIRM.getMessage()),
                                ComponentUtil.of(MessageKeys.DIALOG_CONFIRM_LORE.getMessage()),
                                100,
                                DialogAction.customClick(
                                        callback::accept,
                                        ClickCallback.Options.builder()
                                                .uses(1)
                                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                .build()
                                )),

                        ActionButton.create(
                                ComponentUtil.of(MessageKeys.DIALOG_CANCEL.getMessage()),
                                ComponentUtil.of(MessageKeys.DIALOG_CANCEL_LORE.getMessage()),
                                100,
                                null)
                ))
        );
    }
}