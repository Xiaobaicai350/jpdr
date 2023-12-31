package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    ShopServiceImpl shopService;
    @Test
    void testSaveShop(){
        shopService.saveShop2Redis(1L,10L);
    }
    //导入店铺数据到GEO
    @Test
    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
//    测试百万数据的统计
    @Test
    void testHyperLogLog(){
        String[] users = new String[1000];
        int index=0;
        for(int i=1;i<=1000000;i++){
            users[index++]="user_"+i;
            //每1000条发送一次
            if(i%10000==0){
                //给index归零
                index=0;
                stringRedisTemplate.opsForHyperLogLog().add("hll1",users);
            }
        }
        //统计数量
        Long size=stringRedisTemplate.opsForHyperLogLog().size("hll1");
        //打印出来的结果是997593条,因为每一个用户的key都不一样，一共有10000000个用户，这样的误差是可以接收的
        //但是特别在意这个误差的话就不能用这个了
        System.out.println("size="+size);
    }
}
