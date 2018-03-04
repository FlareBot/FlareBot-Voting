package stream.flarebot.flarebotvoting;

public class Voter {

    private long userId;
    private String tag;
    private int monthlyVotes;
    private int yearlyVotes;

    private int cookies;

    Voter(long userId, String tag, int monthlyVotes, int yearlyVotes, int cookies) {
        this.userId = userId;
        this.tag = tag;
        this.monthlyVotes = monthlyVotes;
        this.yearlyVotes = yearlyVotes;
        this.cookies = cookies;
    }

    public long getId() {
        return userId;
    }

    public String getTag() {
        return tag;
    }

    public int getMonthlyVotes() {
        return monthlyVotes;
    }

    public void setMonthlyVotes(int monthlyVotes) {
        this.monthlyVotes = monthlyVotes;
    }

    public int getYearlyVotes() {
        return yearlyVotes;
    }

    public void setYearlyVotes(int yearlyVotes) {
        this.yearlyVotes = yearlyVotes;
    }

    public int getCookies() {
        return cookies;
    }

    public void incrementVotes() {
        this.monthlyVotes++;
        this.yearlyVotes++;
    }

    public void giveCookies(int i) {
        this.cookies += i;
    }
}
