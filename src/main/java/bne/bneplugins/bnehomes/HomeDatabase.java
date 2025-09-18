package bne.bneplugins.bnehomes;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface HomeDatabase {
    void connect() throws SQLException;
    void disconnect() throws SQLException;
    void saveHome(Home home) throws SQLException;
    Home getHome(UUID player, String name) throws SQLException;
    void deleteHome(UUID player, String name) throws SQLException;
    List<String> getAllHomeNames(UUID player) throws SQLException;
}
