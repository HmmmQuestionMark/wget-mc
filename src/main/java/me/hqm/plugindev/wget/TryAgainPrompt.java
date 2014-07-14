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

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

public class TryAgainPrompt extends StringPrompt {

    private ConfirmPrompt.Type type;

    TryAgainPrompt(ConfirmPrompt.Type type) {
        this.type = type;
    }

    @Override
    public String getPromptText(ConversationContext conversationContext) {
        return ChatColor.YELLOW + "(click an option)";
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String s) {
        if ("yes".equalsIgnoreCase(s)) {
            return new ConfirmPrompt(type);
        } else if (ConfirmPrompt.Type.REGISTER.equals(type) && "redo".equalsIgnoreCase(s)) {
            return new PasswordPrompt();
        }
        context.getForWhom().sendRawMessage(ChatColor.YELLOW + "Registration cancelled.");
        return null;
    }
}
