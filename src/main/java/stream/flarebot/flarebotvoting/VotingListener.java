package stream.flarebot.flarebotvoting;

import stream.flarebot.webhook_distributor.WebHookListener;
import stream.flarebot.webhook_distributor.events.WebHookBatchReceiveEvent;
import stream.flarebot.webhook_distributor.events.WebHookReceiveEvent;

public class VotingListener extends WebHookListener {

    @Override
    public void onWebHookReceive(WebHookReceiveEvent e) {

    }

    @Override
    public void onBatchWebHookReceive(WebHookBatchReceiveEvent e) {
    }
}
