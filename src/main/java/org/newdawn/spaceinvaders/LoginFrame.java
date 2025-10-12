package org.newdawn.spaceinvaders;

import org.newdawn.spaceinvaders.multiplay.Login;
import org.newdawn.spaceinvaders.multiplay.communication.LoginRequest;
import org.newdawn.spaceinvaders.multiplay.communication.SignUpRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class LoginFrame {
    JFrame frame;
    private JPanel signinPanel=null, signupPanel=null ;
    User user;
    private final Game game;

    private Sprite loginBackground;
    JButton loginButton;
    JButton signupButton;
    JTextField userId;
    JPasswordField password;
    boolean loginStatus = false;
    public LoginFrame(Game game){
        this.game = game;
    }
    public Login login = new Login();

    public void startlogin() {
        loginBackground = SpriteStore.get().getSprite("loginBackground.png");
        ImageIcon signInIcon = new ImageIcon(getClass().getClassLoader().getResource("button/signInBtn.png"));
        ImageIcon signUpIcon = new ImageIcon(getClass().getClassLoader().getResource("button/signUpBtn.png"));
        ImageIcon signInIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/signInBtn_hover.png"));
        ImageIcon signUpIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/signUpBtn_hover.png"));

        frame = new JFrame("로그인");
        this.signinPanel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                loginBackground.draw(g,0,0);
            }
        };
        signinPanel.setPreferredSize(new Dimension(800,600));
        signinPanel.setLayout(null);

        signupButton = new JButton(signUpIcon);
        signupButton.setBounds(300,444,200,50);
        signupButton.setBorderPainted(false);       // 버튼 테두리 설정 해제
        signupButton.setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        signupButton.setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제
        signupButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                signupButton.setIcon(signUpIcon_hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                signupButton.setIcon(signUpIcon);
            }
        });

        loginButton = new JButton(signInIcon);
        loginButton.setBounds(300,377,200,50);
        loginButton.setBorderPainted(false);       // 버튼 테두리 설정 해제
        loginButton.setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        loginButton.setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                loginButton.setIcon(signInIcon_hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                loginButton.setIcon(signInIcon);
            }
        });

        userId = new JTextField(10);
        userId.setBounds(300,222,200,50);
        userId.setOpaque(false); // 1. 배경을 투명하게 설정
        userId.setBorder(new EmptyBorder(0, 5, 0, 5)); // 2. 테두리를 투명하고 안쪽 여백을 줌
        userId.setForeground(Color.BLACK); // 3. 글자 색 변경
        userId.setFont(new Font("SansSerif", Font.BOLD, 15)); // 4. 폰트 설정
        userId.setHorizontalAlignment(JTextField.LEFT); // 5. 텍스트 가운데 정렬

        password = new JPasswordField(10);
        password.setBounds(300,302,200,50);
        password.setOpaque(false); // 1. 배경을 투명하게 설정
        password.setBorder(new EmptyBorder(0, 5, 0, 5)); // 2. 테두리 제거
        password.setForeground(Color.BLACK); // 3. 글자 색 변경
        password.setFont(new Font("SansSerif", Font.BOLD, 15)); // 4. 폰트 설정
        password.setHorizontalAlignment(JPasswordField.LEFT); // 5. 텍스트 가운데 정렬

        signinPanel.add(signupButton);
        signinPanel.add(loginButton);
        signinPanel.add(userId);
        signinPanel.add(password);

        frame.add(signinPanel);
        frame.pack();
        frame.setVisible(true);
        AuthLogin();
        signupButton.addActionListener(e->{
            signUp();
        });

    }
    public void signUp(){
        ImageIcon signUpIcon = new ImageIcon(getClass().getClassLoader().getResource("button/signUpBtn.png"));
        ImageIcon backIcon = new ImageIcon(getClass().getClassLoader().getResource("button/backBtn.png"));
        ImageIcon signUpIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/signUpBtn_hover.png"));
        ImageIcon backIcon_hover = new ImageIcon(getClass().getClassLoader().getResource("button/hover/backBtn_hover.png"));
        loginBackground = SpriteStore.get().getSprite("loginBackground.png");

        signupPanel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                loginBackground.draw(g, 0, 0);
            }
        };
        signupPanel.setPreferredSize(new Dimension(800,600));
        signupPanel.setLayout(null);

        JButton SUbtn = new JButton(signUpIcon);
        SUbtn.setBounds(300,377,200,50);
        SUbtn.setBorderPainted(false);       // 버튼 테두리 설정 해제
        SUbtn.setFocusPainted(false);        // 포커스가 갔을 때 생기는 테두리 설정 해제
        SUbtn.setContentAreaFilled(false);   // 버튼 영역 배경 표시 해제
        SUbtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // 아이콘을 hover 이미지로 변경
                SUbtn.setIcon(signUpIcon_hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 아이콘을 기본 이미지로 복원
                SUbtn.setIcon(signUpIcon);
            }
        });


        JTextField username = new JTextField(10);
        username.setBounds(300,222,200,50);
        username.setOpaque(false); // 1. 배경을 투명하게 설정
        username.setBorder(new EmptyBorder(0, 5, 0, 5)); // 2. 테두리를 투명하고 안쪽 여백을 줌
        username.setForeground(Color.BLACK); // 3. 글자 색 변경
        username.setFont(new Font("SansSerif", Font.BOLD, 15)); // 4. 폰트 설정
        username.setHorizontalAlignment(JTextField.LEFT); // 5. 텍스트 가운데 정렬


        JTextField password = new JTextField(10);
        password.setBounds(300,302,200,50);
        password.setOpaque(false); // 1. 배경을 투명하게 설정
        password.setBorder(new EmptyBorder(0, 5, 0, 5)); // 2. 테두리 제거
        password.setForeground(Color.BLACK); // 3. 글자 색 변경
        password.setFont(new Font("SansSerif", Font.BOLD, 15)); // 4. 폰트 설정
        password.setHorizontalAlignment(JPasswordField.LEFT); // 5. 텍스트 가운데 정렬


        signupPanel.add(SUbtn);
        signupPanel.add(username);
        signupPanel.add(password);

        frame.setContentPane(signupPanel);
        frame.revalidate();
        frame.repaint();

        SUbtn.addActionListener(e->{

            game.performLoginOrSignUp(new SignUpRequest(username.getText(), password.getText()));
        });
    }

    public void AuthLogin(){
        loginButton.addActionListener(e -> {

            game.performLoginOrSignUp(new LoginRequest(userId.getText(), new String(password.getPassword())));
        });
    }
}


