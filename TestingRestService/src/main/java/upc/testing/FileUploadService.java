package upc.testing;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/upload")
public class FileUploadService {

	private static final String UPLOAD_FOLDER = "../testing/output/test_result";

	@CrossOrigin
	@PostMapping(value = "/PostResult", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Testing result")
    public void uploadFile(@RequestBody String json) {
		write_to_file(json);
    }

	private void write_to_file(String data){
		try (FileOutputStream outputStream = new FileOutputStream(UPLOAD_FOLDER)) {
			byte[] strToBytes = data.getBytes();
			outputStream.write(strToBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@CrossOrigin
	@GetMapping(value = "/GetResult", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Testing result")
	public String uploadFile() {
		return read_file();
	}

	public String read_file() {
		String data = null;

		try {
			Path path = Paths.get(UPLOAD_FOLDER);
			data = Files.readAllLines(path).get(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
}
