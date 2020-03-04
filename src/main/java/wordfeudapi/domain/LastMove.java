package wordfeudapi.domain;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LastMove {

    private String move_type;
    private long user_id;
    private Object[][] move;
    private String main_word;
    private int points;

    public static LastMove fromJson(final String json) {
        return new Gson().fromJson(json, LastMove.class);
    }

    public String getMove_type() {
        return move_type;
    }

    public long getUser_id() {
        return user_id;
    }

    public List<Tile> getMove() {
        if (move == null) {
            return Collections.emptyList();
        }
        ArrayList<Tile> tiles = new ArrayList<>();
        for (Object[] objects : move) {
            tiles.add(new Tile(objects));
        }
        return tiles;
    }

    public String getMain_word() {
        return main_word;
    }

    public int getPoints() {
        return points;
    }
}
