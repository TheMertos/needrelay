import { Box, Paper } from '@mantine/core';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import 'leaflet/dist/leaflet.css';
import { useEffect } from 'react';
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';

// Fix default marker icons under Vite bundling.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

export type LocationMapMode = 'picker' | 'readonly';

export interface LocationMapProps {
  mode: LocationMapMode;
  lat: number;
  lng: number;
  onChange?: (lat: number, lng: number) => void;
  height?: number;
}

/**
 * Click handler for picker mode.
 *
 * @param onChange callback with new coordinates
 */
function MapClickHandler({
  onChange,
}: {
  onChange?: (lat: number, lng: number) => void;
}) {
  useMapEvents({
    click(event) {
      onChange?.(event.latlng.lat, event.latlng.lng);
    },
  });
  return null;
}

/**
 * Recenters the map when coordinates change.
 *
 * @param lat latitude
 * @param lng longitude
 */
function Recenter({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap();
  useEffect(() => {
    map.setView([lat, lng]);
  }, [lat, lng, map]);
  return null;
}

/**
 * Shared OpenStreetMap location component (picker or readonly).
 *
 * @param props map mode and coordinates
 * @returns Mantine-wrapped Leaflet map
 */
export function LocationMap({
  mode,
  lat,
  lng,
  onChange,
  height = 320,
}: LocationMapProps) {
  return (
    <Paper
      withBorder
      radius="md"
      style={{ overflow: 'hidden', position: 'relative', zIndex: 0, isolation: 'isolate' }}
    >
      <Box style={{ height }}>
        <MapContainer
          center={[lat, lng]}
          zoom={13}
          style={{ height: '100%', width: '100%', zIndex: 0 }}
          scrollWheelZoom={mode === 'picker'}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker
            position={[lat, lng]}
            draggable={mode === 'picker'}
            eventHandlers={
              mode === 'picker'
                ? {
                    dragend: (event) => {
                      const marker = event.target as L.Marker;
                      const position = marker.getLatLng();
                      onChange?.(position.lat, position.lng);
                    },
                  }
                : undefined
            }
          />
          {mode === 'picker' ? <MapClickHandler onChange={onChange} /> : null}
          <Recenter lat={lat} lng={lng} />
        </MapContainer>
      </Box>
    </Paper>
  );
}
