package upc.testing;

import java.io.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value="/upload")
public class FileUploadService {

	private static final String UPLOAD_FOLDER = "../testing/output/";
	
	public FileUploadService() {}
	
	@Context
    private UriInfo context;

	@CrossOrigin
	@RequestMapping(value = "/Test", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA)
	@ApiOperation(value = "Testing result")
    public Response uploadFile(@RequestParam("result") MultipartFile file,
							   @RequestParam("info") JSONObject json) {

		System.out.println("Enter");
		try {
			InputStream reader = file.getInputStream();
			saveToFile(reader, UPLOAD_FOLDER + "test_result");
			System.out.println(json.toString());

			try (FileWriter file2 = new FileWriter("../testing/output/test_info")) {
				file2.write(json.toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

        return Response.status(200).entity("File saved to " /*+ uploadedFileLocation*/).build();
    }

	private void saveToFile(InputStream inStream, String target) throws IOException {
		OutputStream out = null;
		try {
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(new File(target));
			while ((read = inStream.read(bytes)) != -1) {
				System.out.println("writing file");
				out.write(bytes, 0, read);
			}
			out.flush();
		} finally {
			if (out != null) out.close();
		}
	}

	@CrossOrigin
	@RequestMapping(value = "/GetResult", method = RequestMethod.GET)
	@ApiOperation(value = "Testing result")
	public ResponseEntity<?> uploadFile() {

		String info = "";
		String result = "";
		String line = "";
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader("../testing/output/test_info");
			bufferedReader = new BufferedReader(fileReader);
			while ((line = bufferedReader.readLine()) != null) {
				info = info.concat(line);
			}

			fileReader = new FileReader("../testing/output/test_result");
			bufferedReader = new BufferedReader(fileReader);
			while ((line = bufferedReader.readLine()) != null) {
				result = result.concat(line);
			}

			JSONObject info_json = new JSONObject(info);
			JSONObject result_json = new JSONObject(result);

			JSONObject aux = new JSONObject();
			aux.put("info", info_json);
			aux.put("result_json", result_json);

			return new ResponseEntity<>(aux.toString(),HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (fileReader != null) fileReader.close();
				if (bufferedReader != null) bufferedReader.close();
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

	}




}
