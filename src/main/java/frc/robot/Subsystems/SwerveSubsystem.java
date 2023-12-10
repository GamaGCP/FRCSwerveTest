package frc.robot.Subsystems;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.ModuleConstants;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.interfaces.Gyro;
import frc.Lib.config.*;
import frc.robot.Constants;

public class SwerveSubsystem extends SubsystemBase {
    private ShuffleboardTab swerveTab = Shuffleboard.getTab("SDS Swerve");
    private ShuffleboardTab modualTab = Shuffleboard.getTab("Modual Info");

    //shuffleboard telementry

    private GenericEntry xSpeedEntry =
    swerveTab.add("Controler xSpeed", 0).getEntry();

    private GenericEntry ySpeedEntry =
    swerveTab.add("Controler ySpeed", 0).getEntry();

    private GenericEntry rotSpeedEntry =
    swerveTab.add("Controler rotSpeed", 0).getEntry();

    /*private GenericEntry frontRightStateEntry =
    swerveTab.add("FR State V", 0).getEntry();

    private GenericEntry frontLefttStateEntry =
    swerveTab.add("FL State V", 0).getEntry();

    private GenericEntry backRightStateEntry =
    swerveTab.add("BR State V", 0).getEntry();

    private GenericEntry backLeftStateEntry =
    swerveTab.add("BL State V", 0).getEntry();*/

    private GenericEntry gyroEntry =
    swerveTab.add("Gyro Heading", 0).getEntry();

    private SwerveModule[] mSwerveMods;

    private SwerveDriveOdometry driveOdometry;

    private Field2d field;

    
    private final AHRS gyro;

//create the swereve moduals
    public SwerveSubsystem()
    {

        gyro = new AHRS();
        zeroGyro();
    mSwerveMods =
        new SwerveModule[] 
        {
          new SwerveModule(0, Constants.DriveConstants.Mod0.constants, modualTab.getLayout("Front Left Module", BuiltInLayouts.kList).withSize(2, 4).withPosition(0, 0)),
          new SwerveModule(1, Constants.DriveConstants.Mod1.constants, modualTab.getLayout("Back Right Module", BuiltInLayouts.kList).withSize(2, 4).withPosition(2, 4)),
          new SwerveModule(2, Constants.DriveConstants.Mod2.constants, modualTab.getLayout("Back Left Module", BuiltInLayouts.kList).withSize(2, 4).withPosition(2, 0)),
          new SwerveModule(3, Constants.DriveConstants.Mod3.constants, modualTab.getLayout("Front Right Module", BuiltInLayouts.kList).withSize(2, 4).withPosition(0, 4))
        };

        //Timer.delay(1);
    resetToAbsolute2();

        SwerveDriveOdometry driveOdometry = 
      new SwerveDriveOdometry(DriveConstants.kDriveKinematics, getYaw(), getModulePositions());

      field = new Field2d();
    SmartDashboard.putData("Field", field);
    }
    public void resetToAbsolute2(){
        for(SwerveModule mod : mSwerveMods){
          mod.resetToAbsolute();
        }
      }
       public Rotation2d getYaw() {
    return (Constants.DriveConstants.invertGyro)
        ? Rotation2d.fromDegrees(360 - gyro.getYaw())
        : Rotation2d.fromDegrees(gyro.getYaw());
  } 

  public void zeroGyro() {
    gyro.reset();
  }

      public SwerveModulePosition[] getModulePositions() 
      {
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for(SwerveModule mod : mSwerveMods)
        {
            positions[mod.moduleNumber] = mod.getPosition();
        }
        return positions;
      }

    public Pose2d getPose() 
      {
        return driveOdometry.getPoseMeters();
      }

      public void resetOdometry(Pose2d pose){
        driveOdometry.resetPosition(getYaw(), getModulePositions(), pose);
      }

      //@SuppressWarnings("ParameterName")
      public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative)
      {
        var swerveModuleStates = 
          DriveConstants.kDriveKinematics.toSwerveModuleStates(
            fieldRelative
              ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, getYaw())
              : new ChassisSpeeds(xSpeed, ySpeed, rot));
        SwerveDriveKinematics.desaturateWheelSpeeds(
          swerveModuleStates,
           DriveConstants.kPhysicalMaxSpeedMeterPerSecond);
    
           for (SwerveModule mod : mSwerveMods) 
           {
            mod.SetDesiredState(swerveModuleStates[mod.moduleNumber]);
           }
    
        // Telemetry
        xSpeedEntry.setDouble(xSpeed);
        ySpeedEntry.setDouble(ySpeed);
        rotSpeedEntry.setDouble(rot);
            
      }
      public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(
          desiredStates, DriveConstants.kPhysicalMaxSpeedMeterPerSecond);
          for (SwerveModule mod : mSwerveMods) 
           {
            mod.SetDesiredState(desiredStates[mod.moduleNumber]);
           }
      }

      public void resetEncoders(){
        for (SwerveModule mod : mSwerveMods){
            mod.resetDriveEncoders();
            mod.resetTurnEncoders();
        }
      }

      public double getTurnRate(){
        return gyro.getRate() * (DriveConstants.invertGyro ? -1.0 : 1.0);
      }
    

    public void periodic() 
        {
        driveOdometry.update(getYaw(), getModulePositions());
        field.setRobotPose(getPose());
    
        for (SwerveModule mod : mSwerveMods) 
            {
    
          SmartDashboard.putNumber(
              "Mod " + mod.moduleNumber + " Cancoder", mod.getCanCoder().getDegrees());
          SmartDashboard.putNumber(
              "Mod " + mod.moduleNumber + " Integrated", mod.getState().angle.getDegrees());
          SmartDashboard.putNumber(
              "Mod " + mod.moduleNumber + " Velocity", mod.getState().speedMetersPerSecond);
          /*SmartDashboard.putNumber(
              "Mod " + mod.moduleNumber + " Angle Motor Voltage", mod.appliedAngleVoltage());
          SmartDashboard.putNumber(
              "Mod " + mod.moduleNumber + " Drive Motor Voltage", mod.appliedDriveVoltage());*/
            }
        }
      }
