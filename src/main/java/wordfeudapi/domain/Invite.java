package wordfeudapi.domain;

import com.google.gson.Gson;

/**
 * @author Pierre Ingmansson
 */
public class Invite {
    private long id;
    private String inviter;
    private byte ruleset;
    private String board_type;

    public static Invite fromJson(final String json) {
        return new Gson().fromJson(json, Invite.class);
    }

    public long getId() {
        return id;
    }

    public String getInviter() {
        return inviter;
    }

    public RuleSet getRuleset() {
        return RuleSet.fromByte(ruleset);
    }

    public String getBoardType() {
        return board_type;
    }
}
