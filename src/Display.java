package src;

import java.util.HashMap;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import javax.swing.*;

import src.backend.*;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.*;
import java.io.*;
import javax.sound.sampled.*;
import java.net.URL;
import java.util.*;

public class Display extends Canvas implements Runnable{
	private Settings settings = new Settings();
	private Duration target_study = Duration.ofMinutes(Integer.valueOf(settings.getProperty("target_study")));
	private Duration target_break = Duration.ofMinutes(Integer.valueOf(settings.getProperty("target_break")));
	private int sessions = Integer.valueOf(settings.getProperty("sessions"));
	private int curr_sesh = 1;

	private HashMap<String, Color[]> themes = new HashMap<>();
	private String theme = settings.getProperty("theme");

	// {text_color, bg color}
	private Color[] dark = {new Color(222, 226, 230), new Color(52, 58, 64)};
	private Color[] light = {new Color(52, 58, 64), new Color(206, 212, 218)};
	private Color[] green = {new Color(250, 243, 221), new Color(143, 192, 169)};
	private Color[] blue = {new Color(142, 168, 195), new Color(22, 25, 37)};
	private Color[] orange = {new Color(255, 232, 214), new Color(203, 153, 126)};
	private Color[] brown = {new Color(221, 184, 146), new Color(127, 85, 57)};

	private static Duration totalTime = Duration.ofMinutes(0);
	private static Duration prevTotalTime = Duration.ofMinutes(0);
	private static int timer_minutes = 0;
	private static int timer_seconds = 0;

	public static boolean studying = false;
	public static boolean paused = true;
	private static boolean take_break = false;
	private static boolean finished = false;
	public static Instant start_study;
	public static Instant start_break;
	public static Instant start_pause;
	public static Duration study_time = Duration.ofMinutes(0);
	public static Duration break_time = Duration.ofMinutes(0);
	public static Duration pause_time = Duration.ofMinutes(0);
	private Duration time = study_time;

	private Thread thread;
	public JFrame frame;
	private int WIDTH = Integer.valueOf(settings.getProperty("WIDTH"));
	private int HEIGHT = Integer.valueOf(settings.getProperty("HEIGHT"));
	public static boolean running = false;

	private static double targetFPS = 1.0/24.0;
	private static final long serialVersionUTD = 1L;

	public Display(){
		this.frame  = new JFrame();
		Dimension size = new Dimension(WIDTH, HEIGHT);
		this.setPreferredSize(size);
		themes.put("dark", dark);
		themes.put("light", light);
		themes.put("green", green);
		themes.put("blue", blue);
		themes.put("orange", orange);
		themes.put("brown", brown);
	}

	public static void main(String[] args){
		Display display = new Display();
		Input input = new Input();
		display.frame.setTitle("Pomodoro");
		display.frame.add(display);
		display.frame.pack();
		display.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		display.frame.setLocationRelativeTo(null);
		display.frame.setResizable(true);
		display.frame.setVisible(true);
		display.addKeyListener(input);
		display.start();
	}

	public synchronized void start(){
		this.thread = new Thread(this, "Display");
		running = true;
		target_study = target_study.plusSeconds(1);
		target_break = target_break.plusSeconds(1);
		this.thread.start();
	}

	public synchronized void stop(){
		running = false;
		try {
			this.thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void quit(){
		System.exit(0);
	}

	// https://stackoverflow.com/a/63436083/15678539
	public void playSound() {
		File sound = new File("Notification.wav");
    try{
        Clip clip = AudioSystem.getClip();
        clip.open(AudioSystem.getAudioInputStream(sound));
        clip.start();
    } catch (Exception e){
        e.printStackTrace();
    }
	}

	@Override
	public void run(){
		long start_time = System.currentTimeMillis();
		while(running){
			update();
			render();

			double total = (System.currentTimeMillis() - start_time)/1000.0;
			if(total < targetFPS){
				try {
					this.thread.sleep((long)((targetFPS - total)*1000.0));
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}
			start_time = System.currentTimeMillis();
		}
		stop();
	}

	private void render(){
		WIDTH = frame.getSize().width;
		HEIGHT = frame.getSize().height;

		BufferStrategy b = this.getBufferStrategy();
		if(b == null){
			this.createBufferStrategy(3);
			return;
		}

		Graphics2D g2d = (Graphics2D)b.getDrawGraphics();
		g2d.setColor(themes.get(theme)[1]);
		g2d.fillRect(0, 0, WIDTH, HEIGHT);

		Font tag_font = new Font("Serif", Font.PLAIN, 30);
		Font label_font = new Font("Serif", Font.PLAIN, 50);
		Font timer_font = new Font("Serif", Font.PLAIN, 100);
		g2d.setColor(themes.get(theme)[0]);
		g2d.setFont(tag_font);

		// set text to fit the window size
		if (WIDTH <= 600 && HEIGHT <= 300){
			g2d.drawString(curr_sesh + "/" + sessions, 15, 35);
		} else if (WIDTH <= 215){
			g2d.drawString(curr_sesh + "/" + sessions, 15, 35);
		} else if(WIDTH >= 600 || HEIGHT >= 300){
			g2d.drawString("Pomodoro", 15, 35);
			g2d.drawString("-------------", 15, 51);
			g2d.drawString("Total Time: " + totalTime.toMinutes() + ":" + (totalTime.getSeconds() % 60), 15, 75);
			g2d.drawString("Session: " + curr_sesh + "/" + sessions, 15, 105);
		}

		String label_text = "";
		if(paused){
		 label_text = "Paused";
	 } else if(studying){
			label_text = "Studying";
			timer_minutes = (int)(target_study.minus(study_time).toMinutes());
			timer_seconds = (int)(target_study.minus(study_time).minusMinutes(timer_minutes).getSeconds());
		} else if(take_break){
			label_text = "Break";
			timer_minutes = (int)(target_break.minus(break_time).toMinutes());
			timer_seconds = (int)(target_break.minus(break_time).minusMinutes(timer_minutes).getSeconds());
		} else if(finished){
			label_text = "Finished";
		}

		FontMetrics label_metrics = g2d.getFontMetrics(label_font);
		int label_x = (WIDTH - label_metrics.stringWidth(label_text)) / 2;
		int label_y = (((HEIGHT - label_metrics.getHeight()) - 25) + label_metrics.getAscent());
		g2d.setFont(label_font);
		if(HEIGHT >= 300 && WIDTH >= 220){
			this.frame.setTitle("Pomodoro");
			g2d.drawString(label_text, label_x, label_y);
		} else {
			this.frame.setTitle("Pomodoro: " + label_text);
		}

		g2d.setFont(timer_font);

		String timer_text = String.valueOf(timer_minutes) + ":" + String.valueOf(timer_seconds);
		FontMetrics timer_metrics = g2d.getFontMetrics(timer_font);
		int timer_x = (WIDTH - timer_metrics.stringWidth(timer_text)) / 2;
		int timer_y = ((HEIGHT - timer_metrics.getHeight()) / 2) + timer_metrics.getAscent();

		g2d.drawString(timer_text, timer_x, timer_y);
		g2d.dispose();
		b.show();
	}

	private void update(){
		// if paused, dont update the other times
		if(paused){
		} else if(studying){
			study_time = Duration.between(start_study.plusSeconds(pause_time.getSeconds()), Instant.now());
			totalTime = prevTotalTime.plusSeconds(study_time.getSeconds());
			if(study_time.getSeconds() + 1 >= target_study.getSeconds()){
				playSound();
				if(curr_sesh == sessions){
					studying = false;
					take_break = false;
					paused = false;
					finished = true;
					start_study = null;
					start_break = null;
					study_time = Duration.ofMinutes(0);
					break_time = Duration.ofMinutes(0);
					pause_time = Duration.ofMinutes(0);
					timer_minutes = 0;
					timer_seconds = 0;
					time = study_time;
				} else {
					studying = false;
					take_break = true;
					pause_time = Duration.ofMinutes(0);
					prevTotalTime = totalTime;
					start_break = Instant.now();
				}
			}
		} else if(take_break){
			break_time = Duration.between(start_break.plusSeconds(pause_time.getSeconds()), Instant.now());
			totalTime = prevTotalTime.plusSeconds(break_time.getSeconds());
			if(break_time.getSeconds() + 1 >= target_break.getSeconds()){
				playSound();
				studying = true;
				take_break = false;
				curr_sesh += 1;
				pause_time = Duration.ofMinutes(0);
				prevTotalTime = totalTime;
				start_study = Instant.now();
			}
		}
	}
}
