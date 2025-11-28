
package com.orca.pbl4.service.process;

import java.util.Objects;
import java.util.Set;

public class ProcessFilter {
    private String searchText;
    private Set<Character> states;
    private String user;

    public String getSearchText() {
        return searchText;
    }

    public ProcessFilter setSearchText(String searchText) {
        this.searchText = searchText;
        return this;
    }

    public Set<Character> getStates() {
        return states;
    }

    public ProcessFilter setStates(Set<Character> states) {
        this.states = states;
        return this;
    }

    public String getUser() {
        return user;
    }

    public ProcessFilter setUser(String user) {
        this.user = user;
        return this;
    }

    public boolean matchesUser(String userName) {
        if (user == null || user.isBlank()) return true;
        return Objects.equals(user, userName);
    }
}

