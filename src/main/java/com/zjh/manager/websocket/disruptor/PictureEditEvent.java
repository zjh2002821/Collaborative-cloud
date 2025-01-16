package com.zjh.manager.websocket.disruptor;

import com.zjh.manager.websocket.model.PictureEditRequestMessage;
import com.zjh.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * websocket消息事件处理类
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;
    
    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
