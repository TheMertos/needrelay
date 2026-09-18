import { Badge, Box, Button, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { OfferResponse } from '../api/generated/models';
import { OfferStatus } from '../api/generated/models';
import { SurfaceCard } from './SurfaceCard';
import './OfferInbox.css';

/**
 * Offer inbox list with lifecycle and edit/delete actions.
 *
 * @param props.offers offer list
 * @param props.onEdit open edit handler
 * @param props.onMarkComing mark PENDING as COMING
 * @param props.onMarkReceived open receive modal
 * @param props.onCancel cancel PENDING/COMING offer
 * @param props.onDelete delete handler
 * @returns offer inbox panel
 */
export function OfferInbox({
  offers,
  onEdit,
  onMarkComing,
  onMarkReceived,
  onCancel,
  onDelete,
}: {
  offers: OfferResponse[];
  onEdit: (offer: OfferResponse) => void;
  onMarkComing: (offer: OfferResponse) => void;
  onMarkReceived: (offer: OfferResponse) => void;
  onCancel: (offer: OfferResponse) => void;
  onDelete: (offerId: string) => void;
}) {
  const { t } = useTranslation();

  /**
   * Renders action buttons for one offer (shared mobile/desktop content).
   *
   * @param offer offer row
   * @returns action controls
   */
  function offerActions(offer: OfferResponse) {
    if (offer.status === OfferStatus.CANCELLED) {
      return (
        <Button size="sm" color="red" variant="outline" onClick={() => onDelete(offer.id)}>
          {t('request.deleteOffer')}
        </Button>
      );
    }
    return (
      <>
        {offer.status === OfferStatus.PENDING ? (
          <Button size="sm" variant="light" onClick={() => onMarkComing(offer)}>
            {t('offer.markComing')}
          </Button>
        ) : null}
        {offer.status === OfferStatus.PENDING || offer.status === OfferStatus.COMING ? (
          <Button size="sm" color="red" onClick={() => onMarkReceived(offer)}>
            {t('offer.markReceived')}
          </Button>
        ) : null}
        {offer.status === OfferStatus.PENDING || offer.status === OfferStatus.COMING ? (
          <Button size="sm" variant="outline" color="gray" onClick={() => onCancel(offer)}>
            {t('offer.cancel')}
          </Button>
        ) : null}
        {offer.status !== OfferStatus.RECEIVED ? (
          <Button size="sm" variant="light" onClick={() => onEdit(offer)}>
            {t('request.editOffer')}
          </Button>
        ) : null}
        <Button size="sm" color="red" variant="outline" onClick={() => onDelete(offer.id)}>
          {t('request.deleteOffer')}
        </Button>
      </>
    );
  }

  return (
    <SurfaceCard data-testid="offer-inbox" accent="signal">
      <Title order={4} mb="xs">
        {t('request.offers')}
      </Title>
      <Text size="sm" c="dimmed" mb="md">
        {t('request.offersHint')}
      </Text>
      {offers.length === 0 ? (
        <Text size="sm" c="dimmed">
          {t('request.offersEmpty')}
        </Text>
      ) : (
        <Stack gap="sm">
          {offers.map((offer) => (
            <Box
              key={offer.id}
              className="nr-offer-row"
              p="sm"
              style={{
                border: '1px solid var(--nr-border)',
                borderRadius: 4,
                background: 'var(--nr-surface)',
              }}
            >
              <Group justify="space-between" align="flex-start" wrap="wrap">
                <div>
                  <Group gap="xs" mb={4}>
                    <Text fw={700}>{offer.providerName}</Text>
                    <Badge size="sm" variant="light">
                      {t(`offer.status.${offer.status}`)}
                    </Badge>
                  </Group>
                  <Text size="sm">
                    {t('offer.expected')}: × {offer.quantity} · {offer.firstName} {offer.lastName}
                  </Text>
                  {offer.status === OfferStatus.RECEIVED && offer.quantityReceived != null ? (
                    <Text size="sm">
                      {t('offer.received')}: × {offer.quantityReceived}
                    </Text>
                  ) : null}
                  <Text size="sm" c="dimmed">
                    {offer.phone} · {offer.email}
                  </Text>
                  {offer.note ? (
                    <Text size="xs" c="dimmed" lineClamp={2}>
                      {offer.note}
                    </Text>
                  ) : null}
                </div>
                <Stack gap="xs" hiddenFrom="sm">
                  {offerActions(offer)}
                </Stack>
                <Group gap="xs" visibleFrom="sm">
                  {offerActions(offer)}
                </Group>
              </Group>
            </Box>
          ))}
        </Stack>
      )}
    </SurfaceCard>
  );
}
