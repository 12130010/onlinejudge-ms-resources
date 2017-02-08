package onlinejudge.resource.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Authenticator.RequestorType;
import java.util.ArrayList;
import java.util.List;

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
	@Value("${path.problem}")
	String problemPath;
	
	@Value("${path.submit}")
	String submitPath;
	
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
	
	@RequestMapping(value = "/upfile", method=RequestMethod.POST)
	public @ResponseBody MyResponse upfile(@RequestBody GroupResource groupResource){
		MyResponse myResponse = null;
		List<String> listBackupFileName = new ArrayList<>();
		List<String> listNewFileName = new ArrayList<>();
		try{
			for(MyResource resource: groupResource.getListResource()){
				
				if(MyResource.RESOURCE_TYPE_PROBLEM.equals(resource.getResourceType())){
					
					File fileOutput = new File(problemPath + resource.getFileName()); // File will be wrote data into
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
			}
			
			for (String path : listBackupFileName) {
				FileUtils.deleteQuietly(new File(path));
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
}
class ExceptionNeedRollback extends Exception{
	private static final long serialVersionUID = -1694883875581285891L;

	public ExceptionNeedRollback(String message) {
		super(message);
	}
}