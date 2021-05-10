package wooteco.subway.station;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class StationDao {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Station> stationRowMapper = (resultSet, rowNum) -> new Station(
            resultSet.getLong("id"),
            resultSet.getString("name")
    );

    public StationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Station> findByName(String stationName) {
        String sql = "SELECT * FROM STATION WHERE name = ?";
        final List<Station> result = jdbcTemplate.query(sql, stationRowMapper, stationName);
        return result.stream().findAny();
    }

    public Station save(String stationName) {
        String sql = "INSERT INTO STATION (name) values (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, stationName);
            return ps;
        }, keyHolder);

        final long stationId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new Station(stationId, stationName);
    }

    public List<Station> findAll() {
        String sql = "SELECT * FROM STATION";
        return jdbcTemplate.query(sql, stationRowMapper);
    }

    public Optional<Station> findById(long stationId) {
        String sql = "SELECT * FROM STATION WHERE id = ?";
        final List<Station> result = jdbcTemplate.query(sql, stationRowMapper, stationId);
        return result.stream().findAny();
    }

    public void delete(long stationId) {
        String sql = "DELETE FROM STATION WHERE id=?";
        jdbcTemplate.update(sql, stationId);
    }

    public List<Long> findStationIdsInLineByLineId(long lineId) {
        String sql = "SELECT DISTINCT STATION.id AS station_id FROM STATION JOIN SECTION ON SECTION.line_id = ? " +
                "WHERE SECTION.up_station_id = STATION.id OR SECTION.down_station_Id = STATION.id";

        return jdbcTemplate.queryForList(sql, Long.class, lineId);
    }
}
