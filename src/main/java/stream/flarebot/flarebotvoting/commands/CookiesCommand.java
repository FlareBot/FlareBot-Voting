package stream.flarebot.flarebotvoting.commands;

import com.walshydev.jba.commands.Command;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebotvoting.VoteHandler;

public class CookiesCommand implements Command {

    @Override
    public void onCommand(User user, MessageChannel messageChannel, Message message, String[] strings, Member member) {
        messageChannel.sendMessage("You have " + VoteHandler.instance().getVoter(user.getIdLong()).getCookies()
                + " " + VoteHandler.FLARE_COOKIE).queue();
    }

    @Override
    public String getCommand() {
        return "cookies";
    }

    @Override
    public String getDescription() {
        return "See how many cookies you have";
    }
}
