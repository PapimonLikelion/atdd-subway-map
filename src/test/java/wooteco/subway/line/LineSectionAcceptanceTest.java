package wooteco.subway.line;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import wooteco.subway.AcceptanceTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LineSectionAcceptanceTest extends AcceptanceTest {
    @BeforeEach
    void setUpStations() {
        // given
        Map<String, String> station1 = new HashMap<>();
        station1.put("name", "강남역");
        sendPostRequest(station1, "/stations");

        Map<String, String> station2 = new HashMap<>();
        station2.put("name", "성수역");
        sendPostRequest(station2, "/stations");

        Map<String, String> station3 = new HashMap<>();
        station3.put("name", "잠실나루역");
        sendPostRequest(station3, "/stations");

        Map<String, String> station4 = new HashMap<>();
        station4.put("name", "마두역");
        sendPostRequest(station4, "/stations");
    }

    private ExtractableResponse<Response> sendPostRequest(Map<String, ?> body, String requestPath) {
        return RestAssured.given().log().all()
                .body(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post(requestPath)
                .then().log().all()
                .extract();
    }

    @DisplayName("지하철 노선을 구간과 함께 생성한다")
    @Test
    void createLine() {
        // given
        Map<String, Object> line = new HashMap<>();
        line.put("name", "테스트선");
        line.put("color", "red");
        line.put("upStationId", 1);
        line.put("downStationId", 2);
        line.put("distance", "1000");
        //when
        ExtractableResponse<Response> response = sendPostRequest(line, "/lines");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.header("Location")).isNotBlank();
    }

    @DisplayName("미리 등록되어 있지 않은 역은, 노선 생성에 사용될 수 없다.")
    @Test
    void createLineException() {
        // given
        Map<String, Object> line = new HashMap<>();
        line.put("name", "테스트선");
        line.put("color", "red");
        line.put("upStationId", 100);
        line.put("downStationId", 120);
        line.put("distance", "1000");
        //when
        ExtractableResponse<Response> response = sendPostRequest(line, "/lines");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("상행역, 하행역, 거리가 다 채워져서 요청되지 않았다면 예외처리를 한다")
    @Test
    void createLineCheckFullParam() {
        // given
        Map<String, Object> line = new HashMap<>();
        line.put("name", "테스트선");
        line.put("color", "red");
        line.put("upStationId", 100);
        line.put("distance", "1000");
        //when
        ExtractableResponse<Response> response = sendPostRequest(line, "/lines");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("upstationid와 downstationid가 같다면 예외처리를 한다")
    @Test
    void createLineWithSameStationId() {
        // given
        Map<String, Object> line = new HashMap<>();
        line.put("name", "테스트선");
        line.put("color", "red");
        line.put("upStationId", 1);
        line.put("downStationId", 1);
        line.put("distance", "1000");
        //when
        ExtractableResponse<Response> response = sendPostRequest(line, "/lines");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("노선 생성 시 distance가 0이하라면 예외처리한다")
    @Test
    void createLineDistanceCheck() {
        // given
        Map<String, Object> line = new HashMap<>();
        line.put("name", "테스트선");
        line.put("color", "red");
        line.put("upStationId", 1);
        line.put("downStationId", 2);
        line.put("distance", "-10");
        //when
        ExtractableResponse<Response> response = sendPostRequest(line, "/lines");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    private String createTestLine(int upStationId, int downStationId, String distance) {
        Map<String, Object> line = new HashMap<>();
        line.put("name", "테스트선");
        line.put("color", "red");
        line.put("upStationId", upStationId);
        line.put("downStationId", downStationId);
        line.put("distance", distance);
        ExtractableResponse<Response> lineResponse = sendPostRequest(line, "/lines");
        return lineResponse.header("Location").split("/")[2];
    }

    @DisplayName("노선의 상행역 위로 구간을 추가할 수 있다")
    @Test
    void createSectionUpperUpEndStation() {
        // given
        String lineId = createTestLine(2, 3, "5");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 1);
        section.put("downStationId", 2);
        section.put("distance", "5");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("노선의 하행역 아래로 구간을 추가할 수 있다")
    @Test
    void createSectionUnderDownEndStation() {
        // given
        String lineId = createTestLine(1, 2, "5");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 2);
        section.put("downStationId", 3);
        section.put("distance", "5");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("노선의 역들 사이로 구간을 추가할 수 있다")
    @Test
    void createSectionBetweenLineStations() {
        // given
        String lineId = createTestLine(1, 3, "10");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 1);
        section.put("downStationId", 2);
        section.put("distance", "5");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("구간의 상행역, 하행역은 같은 역이 될 수 없다")
    @Test
    void createSectionWithSameStation() {
        // given
        String lineId = createTestLine(1, 2, "5");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 2);
        section.put("downStationId", 2);
        section.put("distance", "5");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("노선의 역들 사이로 구간을 추가할 때, 기존 구간보다 더 큰 거리를 추가하려하면 예외처리한다.")
    @Test
    void createSectionBetweenOverDistanceLimit() {
        // given
        String lineId = createTestLine(1, 3, "10");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 2);
        section.put("downStationId", 3);
        section.put("distance", "20");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("구간 추가를 요청한 노선에, 구간에서 요청한 upStationId, downStationId가 둘 다 없다면 예외처리 한다")
    @Test
    void createSectionBothStationsNotInLineException() {
        // given
        String lineId = createTestLine(1, 2, "10");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 3);
        section.put("downStationId", 4);
        section.put("distance", "20");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("구간 추가를 요청한 노선에, 이미 upStationId와 downStationId가 존재한다면 예외 처리를 한다")
    @Test
    void createSectionStationsAlreadyExists() {
        // given
        String lineId = createTestLine(1, 2, "10");
        //when
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 1);
        section.put("downStationId", 2);
        section.put("distance", "20");
        ExtractableResponse<Response> sectionResponse = sendPostRequest(section, "/lines/" + lineId + "/sections");
        // then
        assertThat(sectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    private ExtractableResponse<Response> sendDeleteRequest(String lineId, String stationId) {
        return RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .delete("/lines/" + lineId + "/sections?stationId=" + stationId)
                .then().log().all()
                .extract();
    }

    @DisplayName("역을 입력해 구간을 삭제한다")
    @Test
    void deleteSection() {
        // given
        String lineId = createTestLine(1, 2, "10");
        Map<String, Object> section1 = new HashMap<>();
        section1.put("upStationId", 2);
        section1.put("downStationId", 3);
        section1.put("distance", "5");
        ExtractableResponse<Response> section1Response = sendPostRequest(section1, "/lines/" + lineId + "/sections");

        Map<String, Object> section2 = new HashMap<>();
        section2.put("upStationId", 3);
        section2.put("downStationId", 4);
        section2.put("distance", "5");
        ExtractableResponse<Response> section2Response = sendPostRequest(section2, "/lines/" + lineId + "/sections");
        //when
        final ExtractableResponse<Response> deleteResponse = sendDeleteRequest(lineId, "2");
        // then
        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("구간이 하나 남은 경우 삭제가 불가하다")
    @Test
    void deleteSectionException() {
        // given
        String lineId = createTestLine(1, 2, "10");
        //when
        final ExtractableResponse<Response> deleteResponse = sendDeleteRequest(lineId, "2");
        // then
        assertThat(deleteResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("역을 삭제하면, 해당 역을 구간으로 가지고 있던 모든 노선에서 삭제한다.")
    @Test
    void deleteEveryLineSectionContainingStation() {
        //given
        String lineId = createTestLine(1, 2, "5");
        Map<String, Object> section = new HashMap<>();
        section.put("upStationId", 2);
        section.put("downStationId", 3);
        section.put("distance", "5");
        sendPostRequest(section, "/lines/" + lineId + "/sections");
        //when
        RestAssured.given().log().all()
                .when()
                .delete("/stations/2")
                .then().log().all()
                .extract();
        //then
        ExtractableResponse<Response> getLineResponse = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + lineId)
                .then().log().all()
                .extract();

        LineResponse lineResponse = getLineResponse.jsonPath().getObject(".", LineResponse.class);

        assertThat(getLineResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(lineResponse.getName()).isEqualTo("테스트선");
        assertThat(lineResponse.getStations().size()).isEqualTo(2);
    }
}
