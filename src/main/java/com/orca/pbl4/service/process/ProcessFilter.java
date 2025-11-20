
package com.orca.pbl4.service.process;

import java.util.Objects;
import java.util.Set;

/**
 * Tham số lọc tiến trình cấp service.
 * Chỉ giữ dữ liệu tối thiểu, logic filter nằm trong ProcessManager/Query helper.
 */
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

    public boolean hasSearch() {
        return searchText != null && !searchText.isBlank();
    }

    public boolean hasStateFilter() {
        return states != null && !states.isEmpty();
    }

    public boolean matchesState(char state) {
        return states == null || states.isEmpty() || states.contains(Character.toUpperCase(state));
    }

    public boolean matchesUser(String userName) {
        if (user == null || user.isBlank()) return true;
        return Objects.equals(user, userName);
    }
}

