package myrobots;

import java.awt.*;
import robocode.*;



public class Team4 extends AdvancedRobot {

	public double turn = 1;
	public double targetBearing = -200;
	public double targetDistance = -1;
	public double targetHeading = 0;
	public double targetVelocity = 0;
	public double lastTargetHeading = 0;
	public double lastTargetVelocity = 0;
	public double targetX = 0;
	public double targetY = 0;
	public double myX;
	public double myY;
	public double noTargetTurn = 1000;
	public double estBulletX = 0;
	public double estBulletY = 0;
	public double fieldWidth = 0;
	public double fieldHeight = 0;
	public double scanNoTargetTurn = 0;
	public double lastTargetX = 0;
	public double lastTargetY = 0;
	public boolean movingForward = true;
	public double hitTimer = 10;
	public double turnDirection = 1;
	public double radarDirection = 1;

	public void run() {
		fieldWidth = getBattleFieldWidth();
		fieldHeight = getBattleFieldHeight();
		setColors(Color.red, Color.blue, Color.green);
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		myX = getX();
		myY = getY();
		addCustomEvent(new Condition("move") {
			public boolean test() {
				return (myX != getX() || myY != getY());
			};
		});
		addCustomEvent(new Condition("nearWall") {
			public boolean test() {
				return (getX() < 100 || getY() < 100
						|| getX() > fieldWidth - 100 || getY() > fieldHeight - 100);
			};
		});
		while (true) {
			setTurnLeft(10000);
			setMaxVelocity(5);
			ahead(10000);
		}
	}

	public void setTargetXY(double targetBearing, double targetDistance) {
		this.targetBearing = targetBearing;
		this.targetDistance = targetDistance;

		double alpha = 90 - this.getHeading() - targetBearing;
		targetX = targetDistance * Math.cos(Math.toRadians(alpha))
				+ this.getX();
		targetY = targetDistance * Math.sin(Math.toRadians(alpha))
				+ this.getY();
		scanNoTargetTurn = noTargetTurn;
	}

	public double getTargetBearing(String type) {
		double alpha = Math.toDegrees(Math.atan((targetY - this.getY())
				/ (targetX - this.getX())));
		if ((targetY - this.getY()) < 0 && (targetX - this.getX()) < 0) {
			alpha = 180 + alpha;
		} else if ((targetY - this.getY()) < 0 && (targetX - this.getX()) > 0) {
			alpha = 360 + alpha;
		} else if ((targetY - this.getY()) > 0 && (targetX - this.getX()) < 0) {
			alpha = 180 + alpha;
		}
		double bearing = 90 - this.getGunHeading() - alpha;
		if (type.equals("heading")) {
			bearing = 90 - this.getHeading() - alpha;
		}
		bearing = (bearing <= 180 ? bearing : bearing - 360);
		bearing = (bearing >= -180 ? bearing : bearing + 360);

		return bearing;
	}

	public double getCenterBearing() {
		double alpha = Math.toDegrees(Math.atan((fieldHeight / 2 - this.getY())
				/ (fieldWidth / 2 - this.getX())));
		if ((fieldHeight / 2 - this.getY()) < 0
				&& (fieldWidth / 2 - this.getX()) < 0) {
			alpha = 180 + alpha;
		} else if ((fieldHeight / 2 - this.getY()) < 0
				&& (fieldWidth / 2 - this.getX()) > 0) {
			alpha = 360 + alpha;
		} else if ((fieldHeight / 2 - this.getY()) > 0
				&& (fieldWidth / 2 - this.getX()) < 0) {
			alpha = 180 + alpha;
		}
		double bearing = 90 - this.getHeading() - alpha;
		bearing = (bearing <= 180 ? bearing : bearing - 360);
		bearing = (bearing >= -180 ? bearing : bearing + 360);

		return bearing;
	}

	public double predictionAngle(double bulletSpeed) {
		double angle = 0;
		double bearing = getHeading() + targetBearing;
		double tankWidth = this.getWidth();
		double toTurn = getHeading() - getGunHeading() + targetBearing;
		double angularChange = 0;
		double velocityChange = 0;
		if (scanNoTargetTurn < 3 && scanNoTargetTurn != 0) {
			angularChange = (targetHeading - lastTargetHeading)
					/ scanNoTargetTurn;
			velocityChange = (targetVelocity - lastTargetVelocity)
					/ scanNoTargetTurn;
			double dTravel = Math.sqrt((targetX - lastTargetX)
					* (targetX - lastTargetX) + (targetY - lastTargetY)
					* (targetY - lastTargetY))
					/ scanNoTargetTurn;

			angularChange = angularChange / dTravel * targetVelocity;
			velocityChange = velocityChange / dTravel * targetVelocity;
		}

		double diagonal = Math.sqrt(fieldWidth * fieldWidth + fieldHeight
				* fieldHeight);
		for (angle = -45; angle < 45; angle += 1) {
			double gunTurn = Math.abs((toTurn + angle) % 180);
			double estTargetX = targetX;
			double estTargetY = targetY;
			for (double time = 1; time < diagonal / bulletSpeed; time++) {
				double myAngle = bearing + angle;
				estTargetX = estTargetX
						+ Math.sin(Math.toRadians((targetHeading + time
								* angularChange)))
						* (targetVelocity + time * velocityChange);
				estTargetY = estTargetY
						+ Math.cos(Math.toRadians((targetHeading + time
								* angularChange)))
						* (targetVelocity + time * velocityChange);
				estBulletX = myX + Math.sin(Math.toRadians(myAngle))
						* (time - gunTurn / Rules.GUN_TURN_RATE) * bulletSpeed;
				estBulletY = myY + Math.cos(Math.toRadians(myAngle))
						* (time - gunTurn / Rules.GUN_TURN_RATE) * bulletSpeed;
				if (estBulletX < 0 || estBulletY < 0 || estBulletX > fieldWidth
						|| estBulletY > fieldHeight) {
					continue;
				}
				double estDistance = Math.sqrt((estTargetX - estBulletX)
						* (estTargetX - estBulletX) + (estTargetY - estBulletY)
						* (estTargetY - estBulletY));
				if (estDistance < 5) {
					return angle;
				}
			}
		}
		return 1000;
	}

	public void shoot() {
		myX = this.getX();
		myY = this.getY();

		double angle = predictionAngle(Rules.getBulletSpeed(2));
		if (angle != 1000) {
			double gunTurn = (getHeading() - getGunHeading() + targetBearing + angle) % 360;
			gunTurn = (gunTurn <= 180 ? gunTurn : gunTurn - 360);
			gunTurn = (gunTurn >= -180 ? gunTurn : gunTurn + 360);
			turnGunRight(gunTurn);
			fire(2);
		} else {
			double gunTurn = (getHeading() - getGunHeading() + targetBearing) % 360;
			gunTurn = (gunTurn <= 180 ? gunTurn : gunTurn - 360);
			gunTurn = (gunTurn >= -180 ? gunTurn : gunTurn + 360);
			setTurnGunRight(gunTurn);
		}
	}


	public void onScannedRobot(ScannedRobotEvent e) {
		
		lastTargetHeading = targetHeading;
		lastTargetVelocity = targetVelocity;
		lastTargetX = targetX;
		lastTargetY = targetY;
		targetBearing = e.getBearing();
		targetDistance = e.getDistance();
		targetHeading = e.getHeading();
		targetVelocity = e.getVelocity();
		
		double radarTurn = (getHeading() - getRadarHeading() + targetBearing) % 360;
		radarTurn = (radarTurn <= 180 ? radarTurn : radarTurn - 360);
		radarTurn = (radarTurn >= -180 ? radarTurn : radarTurn + 360);
		setTurnRadarRight(radarTurn);
		radarDirection *= -1;
		
		setTargetXY(targetBearing, targetDistance);
		if (getGunHeat() == 0) {
			shoot();
		}
		noTargetTurn = 0;
		scan();
	}


	public void onHitByBullet(HitByBulletEvent e) {
		hitTimer = 0;
	}

	public void onHitWall(HitWallEvent e) {
		reverseDirection();
	}

	public void onHitRobot(HitRobotEvent e) {
		reverseDirection();
	}

	public void onCustomEvent(CustomEvent e) {
		if (e.getCondition().getName().equals("move")) {

			double bearing = getTargetBearing("heading");
			hitTimer++;
			if (targetDistance > 400) {
				if (bearing <= 45 && bearing >= -45 && hitTimer >= 10) {
					setMaxVelocity(8);
				} else if (hitTimer >= 10) {
					setMaxVelocity(5);
				}
			} else {
				if (hitTimer >= 10 && hitTimer % 4 == 0) {
					bearing = (bearing - turnDirection * 90) % 360;
					bearing = (bearing <= 180 ? bearing : bearing - 360);
   					bearing = (bearing >= -180 ? bearing : bearing + 360);
    				setTurnRight(bearing);
					setAhead(100000);
					setMaxVelocity(8);
				} else if ( (turnDirection == 1 && bearing <= 135 && bearing >= 45) || (turnDirection == -1 && bearing <= -45 && bearing >= -35) && hitTimer >= 10) {
					setMaxVelocity(8);
					setMaxTurnRate(2);
				} else if (hitTimer >= 10) {
					setMaxVelocity(5);
					setMaxTurnRate(10);
				}
			}
			if (hitTimer == 1) {
				turnDirection = -1 * turnDirection;
				turn = 1;
				setTurnRight(180);
 				setMaxVelocity(8);
			}
			noTargetTurn++;
			if (noTargetTurn > 6 && noTargetTurn % 7 == 0) {
				setTurnRadarRight(360);
				noTargetTurn = 0;
			}

		} else if (e.getCondition().getName().equals("nearWall")) {
			double bearing = getCenterBearing();
			setMaxTurnRate(Rules.MAX_TURN_RATE);
			setTurnRight(bearing);
			setMaxVelocity(8);
			setAhead(40000);
			hitTimer = 1;
		}
	}

	public void reverseDirection() {
		if (movingForward) {
			setMaxVelocity(6);
			setMaxTurnRate(Rules.MAX_TURN_RATE);
			setBack(40000);
			movingForward = false;
		} else {
			setMaxVelocity(6);
			setMaxTurnRate(Rules.MAX_TURN_RATE);
			setAhead(40000);
			movingForward = true;
		}
	}

	public void onPaint(Graphics2D g) {
		g.setColor(Color.white);
		g.drawLine((int) myX, (int) myY, (int) estBulletX, (int) estBulletY);
		g.setColor(Color.red);
		g.fillOval((int) estBulletX - 3, (int) estBulletY - 3, 6, 6);
	}
}