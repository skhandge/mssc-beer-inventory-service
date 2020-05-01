package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {

    private final BeerInventoryRepository beerInventoryRepository;

    public Boolean allocateOrder(BeerOrderDto beerOrderDto){
        log.debug("Allocating order id " + beerOrderDto.getId());

        AtomicInteger totalOrdered = new AtomicInteger();
        AtomicInteger totalAllocated = new AtomicInteger();

        beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
            int orderQuantity = beerOrderLineDto.getOrderQuantity()!=null?beerOrderLineDto.getOrderQuantity():0;
            int allocatedQuantity = beerOrderLineDto.getQuantityAllocated()!=null?beerOrderLineDto.getQuantityAllocated():0;

            if((orderQuantity - allocatedQuantity) > 0){
                allocateBeerOrderLine(beerOrderLineDto);
            }
            totalOrdered.set(totalOrdered.get() + orderQuantity);
            totalAllocated.set(totalAllocated.get() + allocatedQuantity);
        });

        log.debug("Total Ordered " + totalOrdered.get() + "  Total Allocated "+totalAllocated.get());

        return totalOrdered.get() == totalAllocated.get();
    }

    private void allocateBeerOrderLine(BeerOrderLineDto beerOrderLineDto) {
        List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByUpc(beerOrderLineDto.getUpc());

        beerInventoryList.forEach(beerInventory -> {
            int inventory = beerInventory.getQuantityOnHand()==null?0:beerInventory.getQuantityOnHand();
            int orderQty = beerOrderLineDto.getOrderQuantity()==null?0:beerOrderLineDto.getOrderQuantity();
            int allocatedQty = beerOrderLineDto.getQuantityAllocated()==null?0:beerOrderLineDto.getQuantityAllocated();
            int qtyToAllocate = orderQty-allocatedQty;

            if(inventory >= qtyToAllocate){ //full allocation
                inventory = inventory - qtyToAllocate;
                beerOrderLineDto.setQuantityAllocated(orderQty);
                beerInventory.setQuantityOnHand(inventory);

                beerInventoryRepository.save(beerInventory);
            }
            else if(inventory > 0) {
                beerOrderLineDto.setQuantityAllocated(allocatedQty+inventory);
                beerInventory.setQuantityOnHand(0);

                beerInventoryRepository.delete(beerInventory);
            }
        });

    }


}
