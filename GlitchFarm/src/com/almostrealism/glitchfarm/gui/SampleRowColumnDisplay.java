package com.almostrealism.glitchfarm.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.almostrealism.glitchfarm.line.OutputLine;
import com.almostrealism.glitchfarm.obj.Sample;

public class SampleRowColumnDisplay extends JPanel
									implements Runnable,
									ActionListener, KeyListener {
	private int cols;
	private JButton colHeads[];
	private boolean colHeadValues[];
	
	private KeyBoardSampleDisplay keyboardSamples;
	
	private List rows, downKeys;
	
	private boolean stop = true;
	
	public SampleRowColumnDisplay(int cols) {
		super(new GridBagLayout());
		this.cols = cols;
		
		this.colHeads = new JButton[this.cols];
		this.colHeadValues = new boolean[this.cols];
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 1;
		c.gridwidth = 1;
		
		for (int i = 0; i < this.cols; i++) {
			this.colHeads[i] = new JButton();
			this.colHeads[i].setOpaque(true);
			this.colHeads[i].setBackground(Color.green);
//			this.colHeads[i].setMaximumSize(new Dimension(30, 30));
//			this.colHeads[i].setPreferredSize(new Dimension(30, 30));
			this.colHeads[i].addActionListener(this);
			this.colHeads[i].addKeyListener(this);
			this.colHeadValues[i] = true;
			c.gridx++;
			this.add(this.colHeads[i], c);
		}
		
		this.rows = new ArrayList();
		this.downKeys = new ArrayList();
		
		this.addKeyListener(this);
	}
	
	public void setKeyBoardSampleDisplay(KeyBoardSampleDisplay kd) {
		this.keyboardSamples = kd;
	}
	
	public KeyBoardSampleDisplay getKeyBoardSampleDisplay() {
		return this.keyboardSamples;
	}
	
//	public Dimension getPreferredSize() {
//		return getMinimumSize();
//	}
//	
//	public Dimension getMinimumSize() {
//		return new Dimension(400, 50 * (1 + this.rows.size()));
//	}
	
	public void play() { this.playThread().start(); }
	public void stop() { this.stop = true; }
	public boolean isStopped() { return this.stop; }
	public Thread playThread() { return new Thread(this); }
	
	public void run() {
		this.stop = false;
		
		for (int i = 0; !this.stop; i++) {
			if (this.colHeadValues[i]) {
				this.playColumn(i);
			}
			
			if (i == this.colHeadValues.length - 1) i = -1;
		}
	}
	
	// TODO  Synchronize play
	public void playColumn(int index) {
		Iterator itr = this.rows.iterator();
		
		while (itr.hasNext()) ((SampleRowDisplay)itr.next()).playAndWait(index);
	}
	
	public void addSampleRow(Sample samples[], OutputLine line) {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridy = this.rows.size() + 2;
		c.gridwidth = this.cols;
		c.weighty = 1.0;
		
		SampleRowDisplay row = new SampleRowDisplay(samples, line);
		row.setColumnDisplay(this);
		this.rows.add(row);
		this.add(row, c);
	}
	
	public void actionPerformed(ActionEvent e) {
		for (int i = 0; i < this.cols; i++) {
			if (e.getSource() == this.colHeads[i])
				this.toggleColumnHead(i);
		}
	}
	
	public void toggleColumnHead(int index) {
		this.colHeadValues[index] = !this.colHeadValues[index];
		
		if (this.colHeadValues[index])
			this.colHeads[index].setBackground(Color.green);
		else
			this.colHeads[index].setBackground(Color.black);
	}
	
	public int getKeyBoardSampleIndex() {
		Iterator itr = this.downKeys.iterator();
		
		while (itr.hasNext()) {
			int key = ((Integer) itr.next()).intValue();
			
			switch (key) {
				case KeyEvent.VK_1:
					return 1;
				case KeyEvent.VK_2:
					return 2;
				case KeyEvent.VK_3:
					return 3;
				case KeyEvent.VK_4:
					return 4;
				case KeyEvent.VK_5:
					return 5;
				case KeyEvent.VK_6:
					return 6;
				case KeyEvent.VK_7:
					return 7;
				case KeyEvent.VK_8:
					return 8;
				case KeyEvent.VK_9:
					return 9;
			}
		}
		
		return -1;
	}
	
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_P) {
			if (this.stop) {
				this.play();
			} else {
				this.stop();
			}
		}
		
		this.downKeys.add(new Integer(e.getKeyCode()));
	}
	
	public void keyReleased(KeyEvent e) {
		Integer o = new Integer(e.getKeyCode());
		
		while (this.downKeys.contains(o))
			this.downKeys.remove(o);
	}
	
	public void keyTyped(KeyEvent e) { }
}
