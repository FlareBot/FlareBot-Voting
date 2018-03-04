package stream.flarebot.flarebotvoting.commands;

import com.walshydev.jba.commands.Command;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebotvoting.VoteHandler;

public class VoteCommand implements Command {

    @Override
    public void onCommand(User user, MessageChannel channel, Message message, String[] strings, Member member) {
        VoteHandler.instance().addVote(user.getIdLong());
        channel.sendMessage("Voted").queue();
    }

    @Override
    public String getCommand() {
        return "vote";
    }

    @Override
    public String getDescription() {
        return null;
    }
}