package com.firstperson;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;

public class InputHandler implements KeyListener, MouseListener
{
	boolean rightKeyPressed;
	boolean leftKeyPressed;
	boolean upKeyPressed;
	boolean downKeyPressed;
	boolean middleMousePressed;
	int xPosOfMouseDown;
	int yPosOfMouseDown;

	int lastPitch = -1;

	/*
	 * MouseListener and KeyListener are used due to the jumping of the camera usually between the key-pressed movement before a new Focus Point can be set
	 */
	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				e.consume();
				rightKeyPressed = true;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				e.consume();
				leftKeyPressed = true;
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				e.consume();
				upKeyPressed = true;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				e.consume();
				downKeyPressed = true;
				break;
			case KeyEvent.VK_E:
			case KeyEvent.VK_R:
			case KeyEvent.VK_F1:
			case KeyEvent.VK_F:
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
			case KeyEvent.VK_ESCAPE:
			case KeyEvent.VK_SHIFT:
				e.consume();
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				e.consume();
				rightKeyPressed = false;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				e.consume();
				leftKeyPressed = false;
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				e.consume();
				upKeyPressed = false;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				e.consume();
				downKeyPressed = false;
				break;
		}
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent mouseEvent)
	{
		if (mouseEvent.getButton() == MouseEvent.BUTTON2)
		{
			middleMousePressed = true;
			xPosOfMouseDown = mouseEvent.getX();
			yPosOfMouseDown = mouseEvent.getY();
			mouseEvent.consume();
		}
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent mouseEvent)
	{
		if (mouseEvent.getButton() == MouseEvent.BUTTON2)
		{
			middleMousePressed = false;
			xPosOfMouseDown = mouseEvent.getX();
			yPosOfMouseDown = mouseEvent.getY();
			mouseEvent.consume();
		}
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent mouseEvent)
	{
		return mouseEvent;
	}
}
