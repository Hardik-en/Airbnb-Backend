package com.Hardik.projects.AirbnbApp.controller;

import com.Hardik.projects.AirbnbApp.dto.InventoryDto;
import com.Hardik.projects.AirbnbApp.dto.UpdateInventoryDto;
import com.Hardik.projects.AirbnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<InventoryDto>> getAllInventories(@PathVariable Long roomId){
        return ResponseEntity.ok(inventoryService.getAllInventoriesByRoom(roomId));
    }

    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<Void> updateInventory(@PathVariable Long roomId,@RequestBody UpdateInventoryDto updateInventoryDto){
        inventoryService.updateInventory(roomId,updateInventoryDto);
        return ResponseEntity.noContent().build();
    }
}
