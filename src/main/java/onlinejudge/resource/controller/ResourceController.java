package onlinejudge.resource.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Authenticator.RequestorType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import onlinejudge.dto.MyResponse;
import onlinejudge.dto.file.GroupResource;
import onlinejudge.dto.file.MyResource;

@Controller
public class ResourceController {
	
	
	Map<String, String> paths;
	
	
	public ResourceController(@Value("${path.problem}") String problemPath,@Value("${path.submit}") String submitPath) {
		super();
		paths = new HashMap<>();
		paths.put(MyResource.RESOURCE_TYPE_PROBLEM, problemPath);
		paths.put(MyResource.RESOURCE_TYPE_TESTCASE_INPUT, problemPath);
		paths.put(MyResource.RESOURCE_TYPE_TESTCASE_OUTPUT, problemPath);
		paths.put(MyResource.RESOURCE_TYPE_SUBMIT, submitPath);
	}

	private void addPathToMap(){
	}
	
//	@RequestMapping({"/","/about"})
//	public String about(){
//		return "MicroService Resource";
//	}
	
	@RequestMapping({"/","/about"})
	public @ResponseBody GroupResource about(){
		GroupResource group = new GroupResource();
		MyResource resource = new MyResource(new BigInteger("1"), "prob", "abc/prob.txt", new byte[]{65});
		group.add(resource);
		return group;
	}
	/**
	 * #resource-001
	 * @param groupResource
	 * @return
	 */
	@RequestMapping(value = "/upfiles", method=RequestMethod.POST)
	public @ResponseBody MyResponse upfile(@RequestBody GroupResource groupResource){
		MyResponse myResponse = null;
		List<String> listBackupFileName = new ArrayList<>();
		List<String> listNewFileName = new ArrayList<>();
		try{
			for(MyResource resource: groupResource.getListResource()){
				File fileOutput = new File(getPath(resource)); // File will be wrote data into
				boolean isNewFile = !fileOutput.exists();
				if(!isNewFile){ // need backup
					File backupFile = new File(fileOutput.getAbsolutePath()+"." + System.currentTimeMillis());
					try{
						FileUtils.copyFile(fileOutput, backupFile);
						listBackupFileName.add(backupFile.getAbsolutePath());
					}catch (IOException e){
						e.printStackTrace();
						throw new ExceptionNeedRollback(e.getMessage());
					}
				}
				
				try {
					FileUtils.copyInputStreamToFile(resource.inputStream(),fileOutput);
					if(isNewFile)
						listNewFileName.add(fileOutput.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
					throw new ExceptionNeedRollback(e.getMessage());
				}
			}
			
			if(!groupResource.isKeepBackup()){
				for (String path : listBackupFileName) {
					FileUtils.deleteQuietly(new File(path));
				}
			}
			myResponse = MyResponse.builder().success().build();
		}catch(ExceptionNeedRollback e){
			myResponse = MyResponse.builder().fail().build();
			//rollback
			for (String path : listNewFileName) {
				FileUtils.deleteQuietly(new File(path));
			}
			for (String path : listBackupFileName) {
				try {
					FileUtils.copyFile(new File(path), new File(path.substring(0, path.lastIndexOf('.'))));
					FileUtils.deleteQuietly(new File(path));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return myResponse;
	}
	/**
	 * #resource-002
	 * @param groupResource
	 * @return
	 */
	@RequestMapping("/downfiles")
	public @ResponseBody GroupResource download( @RequestBody GroupResource groupResource){
		File file = null;
		byte[] data = null;
		for (MyResource resource : groupResource.getListResource()) {
			 file = FileUtils.getFile(getPath(resource));
			if(file.exists()){
				try {
					data = FileUtils.readFileToByteArray(file);
					resource.setData(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return groupResource;
	}
	
	private String getPath(MyResource resource){
		return paths.get(resource.getResourceType()) + resource.getFileName();
	}
}



class ExceptionNeedRollback extends Exception{
	private static final long serialVersionUID = -1694883875581285891L;

	public ExceptionNeedRollback(String message) {
		super(message);
	}
}