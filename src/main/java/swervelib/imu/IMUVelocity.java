package swervelib.imu;

import com.ctre.phoenix.sensors.WPI_PigeonIMU;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.hal.HALUtil;
import edu.wpi.first.math.filter.LinearFilter;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.ADIS16448_IMU;
import edu.wpi.first.wpilibj.ADIS16470_IMU;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.Notifier;

public class IMUVelocity {
  /**
   * Swerve IMU.
   */
  private final SwerveIMU gyro;
  /**
   * Linear filter used to calculate velocity.
   */
  private final LinearFilter velocityFilter;
  /**
   * WPILib {@link Notifier} to keep IMU velocity up to date.
   */
  private final Notifier  notifier;

  /**
   * Prevents calculation when no previous measurement exists.
   */
  private boolean firstCycle = true;
  /**
   * Tracks the previous loop's recorded time.
   */
  private double timestamp = 0.0;
  /**
   * Tracks the previous loop's position as a Rotation2d. 
   */
  private Rotation2d position = new Rotation2d();
  /**
   * The calculated velocity of the robot based on averaged IMU measurements.
   */
  private double velocity = 0.0;

  /**
   * Constructor for the IMU Velocity.
   *
   * @param gyro The SwerveIMU gyro.
   * @param periodSeconds The rate to collect measurements from the gyro, in the form (1/number of samples per second),
   * make sure this does not exceed the update rate of your IMU.
   * @param averagingTaps The number of samples to used for the moving average linear filter. Higher values will not
   * allow the system to update to changes in velocity, lower values may create a less smooth signal. Expected taps
   * will probably be ~2-8, with the goal of having the lowest smooth value.
   * 
   */
  public IMUVelocity(SwerveIMU gyro, double periodSeconds, int averagingTaps)
  {
    this.gyro = gyro;
    velocityFilter = LinearFilter.movingAverage(averagingTaps);
    notifier = new Notifier(this::update);
    notifier.startPeriodic(periodSeconds);
    timestamp = HALUtil.getFPGATime();
  }

  /**
   * Static factory for IMU Velocity. Supported IMU rates will be as quick as possible
   * but will not exceed 100hz and will use 5 taps (supported IMUs are listed in swervelib's IMU folder). 
   * Other gyroscopes will default to 50hz and 5 taps. For custom rates please use the IMUVelocity constructor.
   *
   * @param gyro The SwerveIMU gyro.
   */
  public static IMUVelocity createIMUVelocity(SwerveIMU gyro)
  {
    // ADIS16448_IMU ~200HZ:
    // https://github.com/wpilibsuite/allwpilib/blob/f82e1c9d4807f4c0fa832fd5bd9f9e90848eb8eb/wpilibj/src/main/java/edu/wpi/first/wpilibj/ADIS16448_IMU.java#L277
    if(gyro.getIMU() instanceof ADIS16448_IMU)
    {
      return new IMUVelocity(gyro, 1.0/100.0, 5);
    }
    // ADIS16470_IMU 200HZ
    // https://github.com/wpilibsuite/allwpilib/blob/f82e1c9d4807f4c0fa832fd5bd9f9e90848eb8eb/wpilibj/src/main/java/edu/wpi/first/wpilibj/ADIS16470_IMU.java#L345
    else if(gyro.getIMU() instanceof ADIS16470_IMU)
    {
      return new IMUVelocity(gyro, 1.0/100.0, 5);
    }
    // ADXRS450_Gyro 2000HZ?
    // https://github.com/wpilibsuite/allwpilib/blob/f82e1c9d4807f4c0fa832fd5bd9f9e90848eb8eb/wpilibj/src/main/java/edu/wpi/first/wpilibj/ADXRS450_Gyro.java#L31
    else if(gyro.getIMU() instanceof ADXRS450_Gyro)
    {
      return new IMUVelocity(gyro, 1.0/100.0, 5);
    }
    // NAX (AHRS): 60HZ
    // https://github.com/kauailabs/navxmxp/blob/5e010ba810bb7f7eaab597e0b708e34f159984db/roborio/java/navx_frc/src/com/kauailabs/navx/frc/AHRS.java#L119C25-L119C61
    else if(gyro.getIMU() instanceof AHRS)
    {
      return new IMUVelocity(gyro, 1.0/60.0, 5);
    }
    // Pigeon2 100HZ
    // https://store.ctr-electronics.com/content/user-manual/Pigeon2%20User's%20Guide.pdf
    else if(gyro.getIMU() instanceof Pigeon2)
    {
      return new IMUVelocity(gyro, 1.0/100.0, 5);
    }
    // Pigeon 100HZ
    // https://store.ctr-electronics.com/content/user-manual/Pigeon%20IMU%20User's%20Guide.pdf
    else if(gyro.getIMU() instanceof WPI_PigeonIMU)
    {
      return new IMUVelocity(gyro, 1.0/100.0, 5);
    }
    // defaults to 50hz and 5 taps
    return new IMUVelocity(gyro, 1.0/50.0, 5);
  }

  /**
   * Update the robot's rotational velocity based on the current gyro position.
   */
  private void update() 
  {
    double newTimestamp = HALUtil.getFPGATime();
    Rotation2d newPosition = Rotation2d.fromRadians(gyro.getRotation3d().getZ());

    synchronized (this) {
      if (!firstCycle) {
        velocity = velocityFilter.calculate(
            (newPosition.minus(position).getRadians()) / (newTimestamp - timestamp));
        }
      firstCycle = false;
      timestamp = newTimestamp;
      position = newPosition;
    }
  }

  /**
   * Get the robot's angular velocity based on averaged meaasurements from the IMU.
   *
   * @return robot's angular velocity in rads/s as a double.
   */
  public synchronized double getVelocity() {
    return velocity;
  }
}