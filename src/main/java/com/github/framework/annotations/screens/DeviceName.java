package com.github.framework.annotations.screens;

public enum DeviceName {
	iPhoneX(375, 812),
	iPad(768, 1024),
	iPadPro(1024, 1366),
	GalaxyS5(360, 640),
	Pixel2XL(411, 823),
	OtherDevice(1024, 1366);
	
	public int width = 0;
	public int height = 0;
	
	DeviceName(int width, int height){
		this.width = width;
		this.height = height;
	}
}
