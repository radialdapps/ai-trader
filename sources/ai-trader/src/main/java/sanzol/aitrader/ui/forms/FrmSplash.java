package sanzol.aitrader.ui.forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import api.client.impl.config.ApiConfig;
import sanzol.aitrader.be.config.Constants;

public class FrmSplash extends JFrame
{
	private static final long serialVersionUID = 1L;

	private static FrmSplash frame;

	private JPanel contentPane;
	private JLabel lblTitle;

	public FrmSplash()
	{
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 380, 240);
		setUndecorated(true);
		setIconImage(Toolkit.getDefaultToolkit().getImage(FrmSplash.class.getResource("/resources/logo.png")));
		setLocationRelativeTo(null);
		setResizable(false);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		contentPane.setBackground(new Color(20, 29, 45));

		setContentPane(contentPane);

		lblTitle = new JLabel(Constants.APP_NAME);
		lblTitle.setForeground(new Color(89, 221, 255));
		lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 30));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblTitle, BorderLayout.CENTER);
	}

	public static void close()
	{
		frame.setVisible(false);
		frame.dispose();
	}

	public static void launch() throws InvocationTargetException, InterruptedException
	{
		// EventQueue.invokeLater(new Runnable()
		EventQueue.invokeAndWait(new Runnable()
		{
			public void run()
			{
				try
				{
					frame = new FrmSplash();
					frame.lblTitle.setText(Constants.APP_NAME + " - " + ApiConfig.MARKET_TYPE);
					frame.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

}
