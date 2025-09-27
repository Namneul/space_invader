package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.awt.*;

public class LoginFrame {
    JFrame frame;
    private JPanel signinPanel=null, signupPanel=null ;
    User user;

    JButton loginButton;
    JButton signupButton;
    JTextField userId;
    JPasswordField password;
    boolean loginStatus = false;
    Login login = new Login();

    UserControll userc = new UserControll();

    public void startlogin() {
        frame = new JFrame("로그인");
        signinPanel = new JPanel();
        signinPanel.setPreferredSize(new Dimension(800,600));
        signinPanel.setLayout(null);

        signupButton = new JButton("회원가입");
        signupButton.setBounds(300,365,200,50);

        loginButton = new JButton("로그인");
        loginButton.setBounds(300,315,200,50);

        userId = new JTextField(10);
        userId.setBounds(300,215,200,50);

        password = new JPasswordField(10);
        password.setBounds(300,265,200,50);

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
        signupPanel = new JPanel();
        signupPanel.setPreferredSize(new Dimension(800,600));
        signupPanel.setLayout(null);

        JButton SUbtn = new JButton("회원가입");
        SUbtn.setBounds(300,315,200,50);

        JTextField username = new JTextField(10);
        username.setBounds(300,215,200,50);

        JTextField password = new JTextField(10);
        password.setBounds(300,265,200,50);

        signupPanel.add(SUbtn);
        signupPanel.add(username);
        signupPanel.add(password);

        frame.setContentPane(signupPanel);
        frame.revalidate();
        frame.repaint();

        SUbtn.addActionListener(e->{
            if(login.signUp(username.getText(),password.getText())){
                frame.dispose();
            }else{
                JOptionPane.showMessageDialog(null,"이미 존재하는 아이디입니다");
            }

        });
    }

    public void AuthLogin(){
        loginButton.addActionListener(e -> {
            user  = new User();
            user.Id = userId.getText();
            user.Password = password.getText();

            if(login.login(user.Id,user.Password)){
                userc.AddUser(user);
                loginStatus = true;
                frame.dispose();
            }else{
                JOptionPane.showMessageDialog(null,"login failed");
            }
        });
    }
}


