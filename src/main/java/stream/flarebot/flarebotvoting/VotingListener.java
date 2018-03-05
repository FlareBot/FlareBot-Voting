package stream.flarebot.flarebotvoting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import stream.flarebot.webhook_distributor.WebHookListener;
import stream.flarebot.webhook_distributor.events.WebHookBatchReceiveEvent;
import stream.flarebot.webhook_distributor.events.WebHookReceiveEvent;

public class VotingListener extends WebHookListener {

    private final String auth = FlareBotVoting.getInstance().getConfig().getString("discordbots-auth");

    @Override
    public void onWebHookReceive(WebHookReceiveEvent e) {
        if (e.getAuthorization() != null && e.getAuthorization().equals(auth))
            handleVote(e.getPayload());
    }

    @Override
    public void onBatchWebHookReceive(WebHookBatchReceiveEvent e) {
        if (e.getAuthorization() == null || !e.getAuthorization().equals(auth)) return;
        for (JsonElement element : e.getWebHooks()) {
            handleVote(element);
        }
    }

    private void handleVote(JsonElement element) {
        if (element instanceof JsonObject) {
            JsonObject object = (JsonObject) element;
            if (object.get("bot").getAsString().equals("225652110493089792")
                    && object.get("type").getAsString().equalsIgnoreCase("upvote")) {
                VoteHandler.instance().addVote(Long.parseLong(object.get("user").getAsString()));
            }
        }
    }
}
