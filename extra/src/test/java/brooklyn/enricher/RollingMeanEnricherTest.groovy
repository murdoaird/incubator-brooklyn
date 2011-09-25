package brooklyn.enricher

import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import brooklyn.entity.LocallyManagedEntity
import brooklyn.entity.basic.AbstractApplication
import brooklyn.entity.basic.EntityLocal
import brooklyn.event.Sensor
import brooklyn.event.basic.BasicAttributeSensor
import brooklyn.management.SubscriptionContext

class RollingMeanEnricherTest {
    
    AbstractApplication app
    
    EntityLocal producer

    Sensor<Integer> intSensor, deltaSensor
    Sensor<Double> avgSensor
    SubscriptionContext subscription
    
    RollingMeanEnricher<Integer> averager

    @BeforeMethod
    public void before() {
        app = new AbstractApplication() {}

        producer = new LocallyManagedEntity(owner:app)

        intSensor = new BasicAttributeSensor<Integer>(Integer.class, "int sensor")
        deltaSensor = new BasicAttributeSensor<Integer>(Integer.class, "delta sensor")
        avgSensor = new BasicAttributeSensor<Double>(Integer.class, "avg sensor")
        
        producer.addEnricher(new DeltaEnricher<Integer>(producer, intSensor, deltaSensor))
        averager = new RollingMeanEnricher<Integer>(producer, deltaSensor, avgSensor, 4)
        producer.addEnricher(averager)
    }

    @AfterMethod
    public void after() {
    }

    @Test
    public void testDefaultAverage() {
        assertEquals(averager.getAverage(), null)
    }
    
    @Test
    public void testZeroWindowSize() {
        averager = new RollingMeanEnricher<Integer>(producer, deltaSensor, avgSensor, 0)
        producer.addEnricher(averager)
        
        averager.onEvent(intSensor.newEvent(producer, 10))
        assertEquals(averager.getAverage(), null)
    }
    
    @Test
    public void testSingleValueAverage() {
        averager.onEvent(intSensor.newEvent(producer, 10))
        assertEquals(averager.getAverage(), 10d)
    }
    
    @Test
    public void testMultipleValueAverage() {
        averager.onEvent(intSensor.newEvent(producer, 10))
        averager.onEvent(intSensor.newEvent(producer, 20))
        averager.onEvent(intSensor.newEvent(producer, 30))
        averager.onEvent(intSensor.newEvent(producer, 40))
        assertEquals(averager.getAverage(), (10+20+30+40)/4d)
    }
    
    @Test
    public void testWindowSizeCulling() {
        averager.onEvent(intSensor.newEvent(producer, 10))
        averager.onEvent(intSensor.newEvent(producer, 20))
        averager.onEvent(intSensor.newEvent(producer, 30))
        averager.onEvent(intSensor.newEvent(producer, 40))
        averager.onEvent(intSensor.newEvent(producer, 50))
        averager.onEvent(intSensor.newEvent(producer, 60))
        assertEquals(averager.getAverage(), (30+40+50+60)/4d)
    }
}