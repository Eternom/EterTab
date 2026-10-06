package fr.eternom.eterTab.paper.listeners;

import fr.eternom.eterTab.paper.Main;
import fr.eternom.eterTab.paper.module.scoreboard.BoardListener;
import fr.eternom.eterTab.paper.module.scoreboard.Boards;
import fr.eternom.eterTab.paper.module.scoreboard.DisplayTask;
import fr.eternom.eterTab.paper.module.sidebar.SidebarPreferences;

public class Events {

    public Events(Main main, Boards boards, DisplayTask display, SidebarPreferences preferences) {
        main.getServer().getPluginManager().registerEvents(new BoardListener(main, boards, display, preferences), main);
    }

}
