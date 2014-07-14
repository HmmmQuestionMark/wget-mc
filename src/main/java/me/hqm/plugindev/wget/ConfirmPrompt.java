// The MIT License (MIT)
//
// Copyright © 2014 Alexander Chauncey (aka HmmmQuestionMark)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
package me.hqm.plugindev.wget;

import mkremins.fanciful.FancyMessage;
import org.apache.commons.codec.digest.DigestUtils;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

public class ConfirmPrompt extends StringPrompt {
    public enum Type {
        REGISTER, LOGIN
    }

    private final Type type;

    public ConfirmPrompt(Type type) {
        this.type = type;
    }

    @Override
    public String getPromptText(ConversationContext context) {
        return ChatColor.YELLOW + "[wget] Please confirm your password:";
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
        if (isInputValid(context, input)) {
            return acceptValidatedInput(context, input);
        } else {
            context.getForWhom().sendRawMessage(ChatColor.RED + "[wget] Passwords do not match.");
            FancyMessage message = new FancyMessage("You can: ").
                    color(ChatColor.YELLOW).
                    then("try again").
                    style(ChatColor.ITALIC).
                    command("yes");
            if (Type.REGISTER.equals(type)) {
                message.then(", ").
                        then("start over").
                        style(ChatColor.ITALIC).
                        command("redo").
                        then(", or ");
            } else {
                message.then(" or ");
            }
            message.then("cancel").
                    style(ChatColor.ITALIC).
                    command("cancel");
            message.send((Player) context.getForWhom());
            return new TryAgainPrompt(type);
        }
    }

    protected boolean isInputValid(ConversationContext context, String s) {
        if (Type.REGISTER.equals(type)) {
            return context.getSessionData("password").equals(s);
        } else {
            return WGET.getUser((Player) context.getForWhom()).passwordHash.equals(DigestUtils.sha512Hex(s));
        }
    }

    protected Prompt acceptValidatedInput(ConversationContext context, String s) {
        if (Type.REGISTER.equals(type)) {
            WGET.register((Player) context.getForWhom(), s);
        } else {
            WGET.login((Player) context.getForWhom());
        }
        return null;
    }
}