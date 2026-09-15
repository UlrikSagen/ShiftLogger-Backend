package shiftlogger.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Map;

@RestController
public class ReadyController{
	private static final Logger log = LoggerFactory.getLogger(ReadyController.class);
	private final DataSource dataSource;

	public ReadyController(DataSource dataSource){
		this.dataSource = dataSource;
	}

	@GetMapping("/ready")
	public Map<String, Object> ready(){
		try (Connection conn = dataSource.getConnection();
		PreparedStatement ps = conn.prepareStatement("SELECT 1");
		ResultSet rs = ps.executeQuery()) {
			boolean ok = rs.next() && rs.getInt(1) == 1;


			if (!ok){
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB query did not return 1");
			}

			return Map.of("ready", true, "db", "ok");
		} catch (Exception e){
			log.error("DB not ready", e);
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB not ready: " + e.getMessage(), e);
		}
	}
}
