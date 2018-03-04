package stream.flarebot.flarebotvoting;

import com.walshydev.jba.Config;
import com.walshydev.jba.JBA;
import com.walshydev.jba.SQLController;
import com.walshydev.jba.scheduler.JBATask;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.entities.TextChannel;
import stream.flarebot.flarebotvoting.commands.CookiesCommand;
import stream.flarebot.flarebotvoting.commands.QuitCommand;
import stream.flarebot.flarebotvoting.commands.VoteCommand;
import stream.flarebot.webhook_distributor.WebHookDistributor;
import stream.flarebot.webhook_distributor.WebHookDistributorBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.concurrent.TimeUnit;

public class FlareBotVoting extends JBA {

    private static FlareBotVoting instance;

    private Config config;

    public static void main(String[] args) {
        (instance = new FlareBotVoting()).init();
    }

    private void init() {
        config = new Config("config");

        new WebHookDistributorBuilder("https://webbyhookies.flarebot.stream", "voting", 7574)
                .addEventListener(new VotingListener())
                .setMaxConnectionAttempts(5)
                .useBatch()
                .build().start();

        setupMySQL(config.getString("mysql.username"), config.getString("mysql.password"), config.getString("mysql.host"),
                config.getString("mysql.database"));

        handleTables();

        init(AccountType.BOT, config.getString("token"), config.getString("prefix"));
    }

    @Override
    public void run() {
        registerCommand(new VoteCommand());
        registerCommand(new CookiesCommand());
        registerCommand(new QuitCommand());

        if (config.exists("topVotersId") && config.exists("monthlyVotersId"))
            VoteHandler.instance().setMessageIds(config.getString("topVotersId"), config.getString("monthlyVotersId"));

        VoteHandler.instance().orderVotes();

        new JBATask("Auto-Save") {
            @Override
            public void run() {
                LOGGER.info("Saving...");
                save();
            }
        }.repeat(TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));
    }

    private void handleTables() {
        try {
            SQLController.runSqlTask(connection -> {
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE IF NOT EXISTS votes_").append(Year.now().getValue()).append(" (")
                        .append("user_id VARCHAR(20) PRIMARY KEY, ");
                for (Month m : Month.values())
                    sb.append(m.name().toLowerCase()).append(" TINYINT(3) DEFAULT 0, ");
                sb.append("total_votes INT(9) DEFAULT 0)");
                connection.createStatement().execute(sb.toString());

                ResultSet set = connection.createStatement().executeQuery("SELECT SUM(total_votes) AS overall_votes, " +
                        "SUM(february) AS month_votes FROM votes_" + getYear());
                set.next();
                VoteHandler.instance().setTotalVotes(set.getInt("month_votes"), set.getInt("overall_votes"));

                ResultSet rs = connection.createStatement().executeQuery("SELECT user_id, user_tag, " + getMonth()
                        + ", total_votes, cookies FROM votes_" + getYear());
                while (rs.next()) {
                    VoteHandler.instance().getVoters().add(new Voter(Long.parseLong(rs.getString("user_id")),
                            rs.getString("user_tag"), rs.getInt(getMonth()), rs.getInt("total_votes"), rs.getInt("cookies")));
                }
                LOGGER.info("Loaded " + VoteHandler.instance().getVoters().size() + " voters");
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getMonth() {
        return LocalDate.now(Clock.systemDefaultZone()).getMonth().name().toLowerCase();
    }

    private int getYear() {
        return Year.now().getValue();
    }

    public TextChannel getVotingChannel() {
        return getClient().getTextChannelById(config.getString("vote-channel"));
    }

    public TextChannel getVoteAnnounceChannel() {
        return getClient().getTextChannelById(config.getString("vote-announce-channel"));
    }

    public void save() {
        try {
            String month = getMonth();
            SQLController.runSqlTask(connection -> {
                for (Voter voters : VoteHandler.instance().getVoters()) {
                    PreparedStatement votes = connection.prepareStatement("INSERT INTO votes_" + getYear() + " " +
                            "(user_id, user_tag, " + month + ", total_votes, cookies) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                            + month + " = VALUES(" + month + "), total_votes = VALUES(total_votes), cookies = VALUES(cookies)");

                    votes.setString(1, String.valueOf(voters.getId()));
                    votes.setString(2, voters.getTag());
                    votes.setInt(3, voters.getMonthlyVotes());
                    votes.setInt(4, voters.getYearlyVotes());
                    votes.setInt(5, voters.getCookies());

                    votes.executeUpdate();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static FlareBotVoting getInstance() {
        return instance;
    }

    public Config getConfig() {
        return config;
    }
}
