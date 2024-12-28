package com.zjh.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zjh.annotation.AuthCheck;
import com.zjh.common.BaseResponse;
import com.zjh.common.ResultUtils;
import com.zjh.constant.UserConstant;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author zjh
 * @version 1.0
 * 文件服务接口
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;

    /**
     *文件上传
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/uploadFile")
    public BaseResponse<String> testUploadFile(@RequestPart MultipartFile multipartFile){
        //1.接收用户上传的文件
        String originalFilename = multipartFile.getOriginalFilename();
        //2.指定上传文件路径
        String filePath = String.format("/test/%s",originalFilename);
        //3.上传文件，通过cos文件存储
        File tempFile = null;
        try {
            tempFile = File.createTempFile(filePath, null);//生成一个临时空文件，防止cos存储出问题，保证文件的完整性
            multipartFile.transferTo(tempFile);//将上传的文件传输在临时文件中
            cosManager.putObject(filePath,tempFile);//上传对象存储中
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload err,filePath {} ",filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文件上传失败！");
        }finally {
            if (tempFile != null){
                boolean delete = tempFile.delete();
                if (!delete){
                    log.error("file upload err,filePath {} ",filePath);
                }
            }

        }
    }

    /**
     * 文件下载
     * @param filePath 要下载文件路径
     * @param response servlet响应处理
     * @throws IOException
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/upload")
    public void testDownLoadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObject object = null;
        try {
            // 从 COS（腾讯云对象存储）中获取指定路径的文件对象
            object = cosManager.getObject(filePath);
            // 获取文件对象的内容流，用于读取文件数据
            COSObjectInputStream cosObjectInputStream = object.getObjectContent();
            byte[] byteArray = IOUtils.toByteArray(cosObjectInputStream);
            //设置响应头，通知浏览器需要接收流式参数
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);
            //通过getOutputStream往前端输出流
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file uploadDown error filePath = "+filePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败！");
        }finally {
            //关闭流
            assert object != null;
            object.close();
        }
    }

}
