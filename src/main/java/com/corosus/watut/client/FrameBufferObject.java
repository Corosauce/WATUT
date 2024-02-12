package com.corosus.watut.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class FrameBufferObject {

	private int frameBufferID;
	//private int colorTextureID;
	//private int depthRenderBufferID;
	
	//dimensions
	private int FRAME_WIDTH;
	private int FRAME_HEIGHT;
	
	public FrameBufferObject(int width, int height){
		FRAME_WIDTH = width;
		FRAME_HEIGHT = height;
		
		frameBufferID = GL30.glGenFramebuffers();
		//colorTextureID = GL30.glGenTextures();
		//depthRenderBufferID = GL30.glGenRenderbuffers();
		
		//frame buffer object
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferID);
		
		//color texture
		//GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTextureID);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
		//GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGB8, FRAME_WIDTH, FRAME_HEIGHT, 0, GL30.GL_RGBA, GL30.GL_INT, (java.nio.ByteBuffer)null);
		GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGB, FRAME_WIDTH, FRAME_HEIGHT, 0, GL30.GL_RGB, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer)null);
		//GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTextureID, 0);
	
		//depth buffer
		//GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBufferID);
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT, FRAME_WIDTH, FRAME_HEIGHT);
		//GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBufferID);
	
		
		//check completeness
		if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE){
			System.out.println("Frame buffer created sucessfully.");
		}
		else
			System.out.println("An error occured creating the frame buffer.");

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	public void Begin(int width, int height){
		/*GL30.glMatrixMode(GL30.GL_PROJECTION);
		GL30.glLoadIdentity();
		GL30.glOrtho(0, width, height, 0, 1, -1);
		GL30.glMatrixMode(GL30.GL_MODELVIEW);
		GL30.glViewport(0, 0, FRAME_WIDTH, FRAME_HEIGHT);*/
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferID);
		//GL30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
	}
	
	public void End(){
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	/*public int getTexture(){
		return colorTextureID;
	}*/
}