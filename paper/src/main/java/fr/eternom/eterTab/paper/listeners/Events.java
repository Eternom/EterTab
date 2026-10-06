package fr.eternom.eterTab.paper.listeners;

import fr.eternom.eterTab.paper.Main;
import fr.eternom.eterTab.paper.module.scoreboard.BoardListener;
import fr.eternom.eterTab.paper.module.scoreboard.Boards;
import fr.eternom.eterTab.paper.module.scoreboard.DisplayTask;

public class Events {

    public Events(Main main, Boards boards, DisplayTask display) {
        main.getServer().getPluginManager().registerEvents(new BoardListener(boards, display), main);
    }

}
