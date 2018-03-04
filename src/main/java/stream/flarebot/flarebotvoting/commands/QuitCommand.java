package stream.flarebot.flarebotvoting.commands;

import com.walshydev.jba.commands.Command;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

public class QuitCommand implements Command {

    @Override
    public void onCommand(User user, MessageChannel messageChannel, Message message, String[] strings, Member member) {
        if (user.getIdLong() == 158310004187725824L)
            System.exit(0);
    }

    @Override
    public String getCommand() {
        return "quit";
    }

    @Override
    public String getDescription() {
        return null;
    }
}
