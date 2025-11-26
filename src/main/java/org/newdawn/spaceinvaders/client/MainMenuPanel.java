package org.newdawn.spaceinvaders.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class MainMenuPanel extends JPanel {

    private transient Image backgroundImage;

    public MainMenuPanel(ActionListener startAction, ActionListener loginAction,
                         ActionListener rankAction, ActionListener onlineAction){
        this.setPreferredSize(new Dimension(800, 600));
        this.setLayout(null);

        URL url = getClass().getClassLoader().getResource("mainBackground.png");
        if (url != null){
            this.backgroundImage = new ImageIcon(url).getImage();
        }

        ImageIcon startIcon = loadIcon("button/startBtn.png");
        ImageIcon loginIcon = loadIcon("button/loginBtn.png");
        ImageIcon rankIcon = loadIcon("button/rankBtn.png");
        ImageIcon onlineIcon = loadIcon("button/onlineBtn.png");

        ImageIcon startIconHover = loadIcon("button/hover/startBtn_hover.png");
        ImageIcon loginIconHover = loadIcon("button/hover/loginBtn_hover.png");
        ImageIcon rankIconHover = loadIcon("button/hover/rankBtn_hover.png");
        ImageIcon onlineIconHover = loadIcon("button/hover/onlineBtn_hover.png");

        JButton startBtn = createButton(293, startIcon, startIconHover, startAction);
        JButton loginBtn = createButton(365, loginIcon, loginIconHover, loginAction);
        JButton rankBtn = createButton(436, rankIcon, rankIconHover, rankAction);
        JButton onlineBtn = createButton(508, onlineIcon, onlineIconHover, onlineAction);

        add(startBtn);
        add(loginBtn);
        add(rankBtn);
        add(onlineBtn);
    }

    private ImageIcon loadIcon(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            return new ImageIcon(); // 빈 아이콘 반환
        }
        return new ImageIcon(url);
    }

    private JButton createButton(int y, ImageIcon icon, ImageIcon hoverIcon, ActionListener action) {
        JButton button = new JButton(icon);
        button.setBounds(275, y, 260, 70);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(action);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { button.setIcon(hoverIcon); }
            @Override
            public void mouseExited(MouseEvent e) { button.setIcon(icon); }
        });
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null){
            g.drawImage(backgroundImage,0,0,this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0,0,getWidth(),getHeight());
        }
    }
}
