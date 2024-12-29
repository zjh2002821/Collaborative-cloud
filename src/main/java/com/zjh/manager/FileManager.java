package com.zjh.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zjh.common.ResultUtils;
import com.zjh.config.CosClientConfig;
import com.zjh.exception.BusinessException;
import com.zjh.exception.ErrorCode;
import com.zjh.exception.ThrowUtils;
import com.zjh.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author zjh
 * @version 1.0
 * 提供通用的本地文件处理操作（本类用于处理对数据库没有强绑定关系的业务处理能力）
 */
@Service
@Slf4j
public class FileManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private CosManager cosManager;

    /**
     * 上传文件至cos存储
     * @param multipartFile 文件资源
     * @param uploadPathPrefix 文件前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix){
        //1.校验文件
        validPicture(multipartFile);
        //2.设置图片上传地址
        String uuid = RandomUtil.randomNumbers(16);//设置uuid
        String originFileName = multipartFile.getOriginalFilename();//获取原始文件名称
        String uploadFileName = String.format("%s_%s.%s",new Date(),uuid,
                                FileUtil.getSuffix(multipartFile.getOriginalFilename()));//当前时间+uuid+文件后缀组成新的文件名
        String uploadFilePath = String.format("/%s/%s",uploadPathPrefix,uploadFileName);//设置上传cos文件路径（路径+新的文件名）
        //3.上传文件，通过cos文件存储
        File tempFile = null;
        try {
            //本地建立临时文件
            tempFile = File.createTempFile(uploadFilePath, null);//生成一个临时空文件，防止cos存储出问题，保证文件的完整性
            multipartFile.transferTo(tempFile);//将上传的文件传输在临时文件中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, tempFile);//上传对象存储中
            //获取上传到cos文件信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            String format = imageInfo.getFormat();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double picScale = NumberUtil.round(width * 1.0 / height,2).doubleValue();//计算宽高比
            //设置图片返回信息
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadFilePath);//获取域名
            uploadPictureResult.setPicName(FileUtil.mainName(originFileName));//提取去除后缀的主文件名
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到cos对象存储失败！",uploadFilePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文件上传失败！");
        }finally {
            if (tempFile != null){
                boolean delete = tempFile.delete();
                if (!delete){
                    log.error("file upload err,filePath {} ",uploadFilePath);
                }
            }

        }
    }

    /**
     * 校验文件
     * @param multipartFile 文件资源
     */
    private void validPicture(MultipartFile multipartFile){
        ThrowUtils.throwIf(multipartFile == null,"文件不能为空！", ErrorCode.PARAMS_ERROR);
        //1.校验文件大小
        long size = multipartFile.getSize();
        //设置允许文件大小范围
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_M,"上传文件大小不能超过2MB！",ErrorCode.PARAMS_ERROR);
        //2.校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //设置文件后缀范围
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(ALLOW_FORMAT_LIST.contains(suffix),"文件类型错误！",ErrorCode.PARAMS_ERROR);
    }
}
