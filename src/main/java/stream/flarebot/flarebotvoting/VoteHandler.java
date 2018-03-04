package stream.flarebot.flarebotvoting;

import com.walshydev.jba.SQLController;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class VoteHandler {

    private static VoteHandler instance;
    public static final String FLARE_COOKIE = "<:flarecookie:373580984941150221>";

    private final Logger logger = LoggerFactory.getLogger(VoteHandler.class);
    private static final int cookie_base = 1;

    private final SecureRandom random = new SecureRandom();

    private String month = getMonthName();

    private Set<Voter> voters = new HashSet<>();
    private int monthlyVotes;
    private int totalVotes;

    private long topVotersId = -1;
    private long monthlyVotersId = -1;

    private Set<User> userCache = new HashSet<>();

    public void addVote(long userId) {
        if (!month.equalsIgnoreCase(getMonthName()))
            resetMonth();

        this.monthlyVotes++;
        this.totalVotes++;

        Voter voter = getVoter(userId);
        int cookies = cookie_base;
        voter.incrementVotes();

        if (random.nextInt(100)+1 <= 25) {
            cookies += 2;
        }

        voter.giveCookies(cookies);

        /*FlareBotVoting.getInstance().getVoteAnnounceChannel().sendMessage(new EmbedBuilder().setTitle("User Voted!")
                .setDescription(voter.getTag() + " has just voted and " +
                        "was awarded " + cookies + " " + FLARE_COOKIE
                        + "! Vote and earn some today! "
                        + "\n[Click here to vote!](https://discordbots.org/bot/225652110493089792/vote)")
                .setColor(Color.pink).build()).queue();
        */

        orderVotes();
    }

    void orderVotes() {
        if (topVotersId == -1 || monthlyVotersId == -1) {
            TextChannel tc = FlareBotVoting.getInstance().getVotingChannel();
            if (topVotersId == -1)
                topVotersId = tc.sendMessage(getTopVotersEmbed(false).build()).complete().getIdLong();
            if (monthlyVotersId == -1)
                monthlyVotersId = tc.sendMessage(getTopVotersEmbed(true).build()).complete().getIdLong();

            FlareBotVoting.getInstance().getConfig().set("topVotersId", String.valueOf(topVotersId));
            FlareBotVoting.getInstance().getConfig().set("monthlyVotersId", String.valueOf(monthlyVotersId));
            FlareBotVoting.getInstance().getConfig().save();
        } else {
            Message topVoters = FlareBotVoting.getInstance().getVotingChannel().getMessageById(topVotersId).complete();
            if (topVoters == null) {
                topVotersId = -1;
                orderVotes();
            } else {
                topVoters.editMessage(getTopVotersEmbed(false).build()).queue();
            }

            Message monthlyVoters = FlareBotVoting.getInstance().getVotingChannel().getMessageById(monthlyVotersId).complete();
            if (monthlyVoters == null) {
                monthlyVotersId = -1;
                orderVotes();
            } else {
                monthlyVoters.editMessage(getTopVotersEmbed(true).build()).queue();
            }
        }
    }

    private int getYear() {
        return Year.now().getValue();
    }

    private Month getMonth() {
        return LocalDate.now(Clock.systemDefaultZone()).getMonth();
    }

    private String getMonthName() {
        return getMonth().name().toLowerCase();
    }

    private String getMonthNamePretty() {
        Month month = getMonth();
        return month.name().charAt(0) + month.name().substring(1).toLowerCase();
    }

    public static VoteHandler instance() {
        if (instance == null)
            instance = new VoteHandler();
        return instance;
    }

    private EmbedBuilder getTopVotersEmbed(boolean monthVoting) {
        EmbedBuilder eb = new EmbedBuilder().setTitle(!monthVoting ? "Top Voters Of " + getYear()
                : getMonthNamePretty() + "'s Top Voters")
                .setColor(new Color(1, 168, 195))
                .setFooter(!monthVoting ? "Total votes: " + totalVotes : "Votes this month: " + monthlyVotes, null);

        StringBuilder sb = new StringBuilder();
        try {
            SQLController.runSqlTask(conn -> {
                int i = 1;
                List<Voter> voterList;
                voterList = voters.stream().sorted(Comparator.comparingInt(voter ->
                        monthVoting ? ((Voter) voter).getMonthlyVotes() : ((Voter) voter).getYearlyVotes()).reversed())
                        .collect(Collectors.toList());

                if (voterList.isEmpty())
                    return;

                for (Voter voter : voterList.subList(0, Math.min(10, voterList.size()))) {
                    sb.append(i).append(". `").append(voter.getTag()).append("` - ")
                            .append(monthVoting ? voter.getMonthlyVotes() : voter.getYearlyVotes()).append(" vote")
                            .append(monthVoting ? (voter.getMonthlyVotes() > 1 ? "s" : "")
                                    : voter.getYearlyVotes() > 1 ? "s" : "").append("\n");
                    i++;
                }

                eb.setDescription(sb.toString());
            });
        } catch (SQLException e) {
            logger.error("Failed to get voting data!", e);
        }
        return eb;
    }

    @Nonnull
    public Voter getVoter(long userId) {
        for (Voter voter : voters)
            if (voter.getId() == userId)
                return voter;

        AtomicReference<Voter> voter = new AtomicReference<>();
        try {
            SQLController.runSqlTask(connection -> {
                ResultSet set = connection.createStatement().executeQuery("SELECT user_id, " + month + ", total_votes, " +
                        "cookies FROM votes_" + getYear() + " WHERE user_id = '" + userId + "'");
                User user = getUser(userId);
                if (set.next()) {
                    voter.set(new Voter(userId, user == null ? "Unknown" : user.getName() + "#" + user.getDiscriminator(),
                            set.getInt(month), set.getInt("total_votes"), set.getInt("cookies")));
                } else
                    voter.set(new Voter(userId, user == null ? "Unknown" : user.getName() + "#" + user.getDiscriminator(),
                            0, 0, 0));
            });
        } catch (SQLException e) {
            throw new IllegalStateException("SQL died while getting voter", e);
        }

        if (voter.get() != null)
            this.voters.add(voter.get());

        return voter.get();
    }

    public void setTotalVotes(int monthlyVotes, int totalVotes) {
        this.monthlyVotes = monthlyVotes;
        this.totalVotes = totalVotes;
    }

    private User getUser(long userId) {
        Optional<User> userOptional = userCache.stream().filter(user -> user.getIdLong() == userId).findFirst();
        if (userOptional.isPresent())
            return userOptional.get();
        for (User user : FlareBotVoting.getInstance().getClient().getUserCache())
            if (user.getIdLong() == userId)
                return user;

        User u = FlareBotVoting.getInstance().getClient().retrieveUserById(userId).complete();
        this.userCache.add(u);
        return u;
    }

    public Set<Voter> getVoters() {
        return voters;
    }

    private void resetMonth() {
        this.month = getMonthName();
        FlareBotVoting.getInstance().save();
        for (Voter voter : voters) {
            voter.setMonthlyVotes(0);
        }
        orderVotes();
        this.monthlyVotes = 0;
    }

    public void setMessageIds(String topVotersId, String monthlyVotersId) {
        this.topVotersId = Long.parseLong(topVotersId);
        this.monthlyVotersId = Long.parseLong(monthlyVotersId);
    }
}
