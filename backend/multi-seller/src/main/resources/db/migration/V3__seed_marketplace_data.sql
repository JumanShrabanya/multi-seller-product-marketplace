INSERT INTO seller (name, city, status) VALUES
    ('BuildRight Supplies', 'Mumbai', 'APPROVED'),
    ('Metro Construction Mart', 'Pune', 'APPROVED'),
    ('Rajasthan Building Depot', 'Jaipur', 'APPROVED'),
    ('New Horizon Traders', 'Nashik', 'PENDING'),
    ('Clearance Building Store', 'Delhi', 'REJECTED');

INSERT INTO product (name, brand, category, unit, description) VALUES
    ('OPC Cement 53 Grade', 'UltraTech', 'Cement', '50 kg bag', 'High-strength ordinary Portland cement for structural concrete work.'),
    ('PPC Cement', 'ACC', 'Cement', '50 kg bag', 'Portland pozzolana cement suited to masonry and general construction.'),
    ('TMT Steel Bar Fe 550D 12 mm', 'Tata Tiscon', 'Steel', '12 m bar', 'Corrosion-resistant ribbed reinforcement bar for RCC structures.'),
    ('TMT Steel Bar Fe 550D 16 mm', 'JSW Neosteel', 'Steel', '12 m bar', 'High-yield reinforcement bar for beams, columns, and slabs.'),
    ('Fly Ash Bricks', 'EcoBrick', 'Bricks', 'piece', 'Uniform fly ash brick for durable masonry walls.'),
    ('River Sand', 'Local Aggregate', 'Sand', 'cubic ft', 'Washed river sand suitable for plastering and concrete mixes.'),
    ('Interior Emulsion Paint', 'Asian Paints', 'Paint', '20 L bucket', 'Washable low-VOC interior wall emulsion in white.'),
    ('Vitrified Floor Tile 600 x 600 mm', 'Kajaria', 'Tiles', 'box', 'Gloss-finish vitrified floor tiles; one box covers approximately 1.44 square metres.'),
    ('UPVC Plumbing Pipe 110 mm', 'Astral', 'Pipes', '3 m length', 'Rigid UPVC pipe for drainage and plumbing installations.'),
    ('Waterproofing Coating', 'Dr. Fixit', 'Chemicals', '20 L bucket', 'Elastomeric waterproof coating for terraces and exterior walls.');

INSERT INTO seller_listing (product_id, seller_id, price, stock, min_order_qty, active) VALUES
    (1, 1, 430.00, 500, 10, TRUE),
    (1, 2, 425.50, 300, 20, TRUE),
    (1, 3, 438.00, 120, 10, TRUE),
    (2, 1, 390.00, 0, 10, TRUE),
    (2, 2, 385.00, 250, 25, TRUE),
    (3, 1, 735.00, 80, 5, TRUE),
    (3, 2, 720.00, 150, 10, TRUE),
    (3, 4, 700.00, 100, 10, TRUE),
    (4, 3, 1240.00, 50, 5, TRUE),
    (5, 1, 8.50, 5000, 500, TRUE),
    (5, 2, 8.20, 3000, 1000, TRUE),
    (6, 3, 52.00, 0, 100, TRUE),
    (6, 1, 55.00, 1000, 100, FALSE),
    (7, 2, 3150.00, 40, 2, TRUE),
    (7, 5, 2900.00, 50, 2, TRUE),
    (8, 1, 740.00, 200, 10, TRUE),
    (8, 3, 725.00, 150, 5, TRUE),
    (9, 2, 680.00, 75, 5, TRUE);
