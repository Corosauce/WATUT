package com.corosus.watut.client.screen;

import java.nio.ByteBuffer;

public record CapturedFrame(ByteBuffer data, int width, int height) {
}
