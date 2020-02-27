package atdd.line.repository;

import atdd.line.domain.Line;
import atdd.line.domain.LineStation;
import atdd.line.domain.TimeTable;
import atdd.station.domain.Duration;
import atdd.station.domain.Station;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class LineRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private LineStationRepository lineStationRepository;

    @Test
    void save() throws Exception {
        final String name = "name!!";
        final TimeTable timeTable = new TimeTable(LocalTime.MIN, LocalTime.MIDNIGHT);
        final int interval = 6;
        final Line line = Line.create(name, timeTable, interval);

        final Line save = lineRepository.save(line);

        final Line find = lineRepository.findById(save.getId()).orElseThrow(EntityNotFoundException::new);

        assertThat(find.getId()).isNotNull();
        assertThat(find.getName()).isEqualTo(name);
        assertThat(find.getTimeTable()).isEqualTo(timeTable);
    }

    @DisplayName("save - 동일한 이름 저장 불가")
    @Test
    void saveSameName() throws Exception {
        final String name = "name!!";
        final TimeTable timeTable = new TimeTable(LocalTime.MIN, LocalTime.MIDNIGHT);
        final int interval = 6;

        lineRepository.save(Line.create(name, timeTable, interval));
        lineRepository.flush();


        assertThatThrownBy(() -> lineRepository.save(Line.create(name, timeTable, interval)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("새로운 station 추가")
    @Test
    void addStation() throws Exception {
        final Station station = Station.create("stationName!!");
        Line line = getSavedLine();


        line.addStation(station);
        lineRepository.flush();


        final List<Station> stations = line.getStations();
        assertThat(stations).hasSize(1);

        final Station addedStation = stations.get(0);
        assertThat(addedStation.getId()).isNotNull();
        assertThat(addedStation.getName()).isEqualTo(station.getName());
    }

    @Test
    void delete() throws Exception {
        final Station station = Station.create("stationName!!");
        Line line = getSavedLine();
        line.addStation(station);
        lineRepository.flush();
        entityManager.clear();

        line.clearStations();
        lineRepository.delete(line);
        entityManager.flush();
        entityManager.clear();


        final List<LineStation> lineStations = lineStationRepository.findAll();
        assertThat(lineStations).isEmpty();
    }

    @Test
    void addSection() {
        final Line line = getSavedLine();
        final Station station1 = Station.create("stationName11");
        final Station station2 = Station.create("stationName22");
        line.addStation(station1);
        line.addStation(station2);

        lineRepository.flush();

        final Duration duration = new Duration(LocalTime.MAX);
        final double distance = 1.5;
        line.addSection(station1.getId(), station2.getId(), duration, distance);

        lineRepository.flush();

        final List<Station> orderedStations = line.getOrderedStations();
        assertThat(orderedStations).hasSize(2);
        assertThat(orderedStations.get(0)).isEqualTo(station1);
        assertThat(orderedStations.get(1)).isEqualTo(station2);

        assertThat(line.getStartStation().get()).isEqualTo(orderedStations.get(0));
    }

    @Test
    void deleteStation() {
        final Line line = getSavedLine();
        final Station station1 = Station.create("stationName11");
        final Station station2 = Station.create("stationName22");
        final Station station3 = Station.create("stationName33");
        final List<Station> expectedStations = Lists.list(station1, station3);

        line.addStation(station1);
        line.addStation(station2);
        line.addStation(station3);
        lineRepository.flush();

        final Duration duration = new Duration(LocalTime.of(1, 1));
        final double distance = 1.5;
        line.addSection(station1.getId(), station2.getId(), duration, distance);
        line.addSection(station2.getId(), station3.getId(), duration, distance);
        lineRepository.flush();


        line.deleteStation(station2.getId());
        lineRepository.flush();

        final List<Station> orderedStations = line.getOrderedStations();
        assertThat(orderedStations).isEqualTo(expectedStations);
    }

    private Line getSavedLine() {
        final String name = "name!!";
        final TimeTable timeTable = new TimeTable(LocalTime.MIN, LocalTime.MIDNIGHT);
        final int interval = 6;
        return lineRepository.save(Line.create(name, timeTable, interval));
    }

}